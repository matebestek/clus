
package si.ijs.kt.clus.ext.ensemble;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import si.ijs.kt.clus.Clus;
import si.ijs.kt.clus.data.ClusSchema;
import si.ijs.kt.clus.data.rows.DataTuple;
import si.ijs.kt.clus.data.rows.RowData;
import si.ijs.kt.clus.error.common.ClusErrorList;
import si.ijs.kt.clus.ext.hierarchical.WHTDStatistic;
import si.ijs.kt.clus.main.ClusOutput;
import si.ijs.kt.clus.main.ClusRun;
import si.ijs.kt.clus.main.ClusStatManager;
import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.main.settings.SettingsEnsemble;
import si.ijs.kt.clus.main.settings.SettingsSSL;
import si.ijs.kt.clus.model.ClusModel;
import si.ijs.kt.clus.model.ClusModelInfo;
import si.ijs.kt.clus.model.processor.ModelProcessorCollection;
import si.ijs.kt.clus.selection.OOBSelection;
import si.ijs.kt.clus.statistic.ClassificationStat;
import si.ijs.kt.clus.statistic.ClusStatistic;
import si.ijs.kt.clus.statistic.RegressionStat;
import si.ijs.kt.clus.util.ClusException;


public class ClusOOBErrorEstimate {

    static HashMap m_OOBPredictions;
    static HashMap<Integer, Integer> m_OOBUsage;
    static boolean m_OOBCalculation;
    int m_Mode;
    Settings m_Settings;
    static HashMap m_OOBVotes; //individual votes for the OOB examples (we need this for automatic selection of threshold for SSL self-training)
    static HashMap OOBMapping; //to store which examples are OOB in which trees
    static ClusReadWriteLock m_LockPredictions = new ClusReadWriteLock();
    static ClusReadWriteLock m_LockUsage = new ClusReadWriteLock();
    static ClusReadWriteLock m_LockCalculation = new ClusReadWriteLock();


    public ClusOOBErrorEstimate(int mode, Settings sett) {
        m_OOBPredictions = new HashMap();
        m_OOBUsage = new HashMap<Integer, Integer>();
        m_OOBCalculation = false;
        m_Mode = mode;
        m_Settings = sett;
        m_OOBVotes = new HashMap();
        OOBMapping = new HashMap();
    }


    private Settings getSettings() {
        return m_Settings;
    }


    public static boolean containsPredictionForTuple(DataTuple tuple) {
        m_LockPredictions.readingLock();
        boolean contains = m_OOBPredictions.containsKey(tuple.hashCode());
        m_LockPredictions.readingUnlock();
        return contains;
    }


    public static double[] getPredictionForRegressionHMCTuple(DataTuple tuple) {
        m_LockPredictions.readingLock();
        double[] pred = (double[]) m_OOBPredictions.get(tuple.hashCode());
        double[] predictions = Arrays.copyOf(pred, pred.length);
        m_LockPredictions.readingUnlock();
        return predictions;
    }


    public static double[][] getPredictionForClassificationTuple(DataTuple tuple) {
        m_LockPredictions.readingLock();
        double[][] pred = (double[][]) m_OOBPredictions.get(tuple.hashCode());
        double[][] predictions = new double[pred.length][];
        for (int i = 0; i < pred.length; i++) {
            predictions[i] = Arrays.copyOf(pred[i], pred[i].length);
        }
        m_LockPredictions.readingUnlock();
        return predictions;
    }


    public static ArrayList getVotesForTuple(DataTuple tuple) {
        return (ArrayList) m_OOBVotes.get(tuple.hashCode());
    }


    public synchronized void postProcessForestForOOBEstimate(ClusRun cr, OOBSelection oob_total, RowData all_data, Clus cl, String addname) throws ClusException, IOException {
        Settings sett = cr.getStatManager().getSettings();
        ClusSchema schema = all_data.getSchema();
        ClusOutput output = new ClusOutput(sett.getGeneric().getAppName() + addname + ".oob", schema, sett);
        setOOBCalculation(true);

        // this is the part for writing the predictions from the OOB estimate
        // should new option in .s file be introduced???
        // ClusStatistic target = getStatManager().createStatistic(ClusAttrType.ATTR_USE_TARGET);
        // PredictionWriter wrt = new PredictionWriter(sett.getAppName() + addname + ".oob.pred", sett, target);
        // wrt.globalInitialize(schema);
        // ClusModelInfo allmi = cr.getAllModelsMI();
        // allmi.addModelProcessor(ClusModelInfo.TRAIN_ERR, wrt);
        // cr.copyAllModelsMIs();
        // wrt.initializeAll(schema);

        calcOOBError(oob_total, all_data, ClusModelInfo.TRAIN_ERR, cr);
        cl.calcExtraTrainingSetErrors(cr);
        output.writeHeader();
        output.writeOutput(cr, true, cl.getSettings().getOutput().isOutTrainError());
        output.close();
        // wrt.close();
        setOOBCalculation(false);
        // m_OOBCalculation = false;
    }


    /*
     * public synchronized void updateOOBTuples(OOBSelection oob_sel, RowData train_data, ClusModel model) throws
     * IOException, ClusException {
     * for (int i = 0; i < train_data.getNbRows(); i++) {
     * if (oob_sel.isSelected(i)) {
     * DataTuple tuple = train_data.getTuple(i);
     * if (existsOOBtuple(tuple))
     * updateOOBTuple(tuple, model);
     * else
     * addOOBTuple(tuple, model);
     * }
     * }
     * }
     */

    public synchronized void updateOOBTuples(OOBSelection oob_sel, RowData train_data, ClusModel model, int modelNo) throws IOException, ClusException {
        for (int i = 0; i < train_data.getNbRows(); i++) {
            if (oob_sel.isSelected(i)) {
                DataTuple tuple = train_data.getTuple(i);
                if (existsOOBtuple(tuple)) {
                    updateOOBTuple(tuple, model);
                }
                else {
                    addOOBTuple(tuple, model);
                }
                updateOOBMapping(tuple, modelNo);
            }
        }
    }


    public synchronized void updateOOBMapping(DataTuple tuple, int treeNumber) {
        if (OOBMapping.containsKey(treeNumber)) {
            ((ArrayList) OOBMapping.get(treeNumber)).add(tuple.hashCode());
        }
        else {
            ArrayList hashCodes = new ArrayList();
            hashCodes.add(tuple.hashCode());
            OOBMapping.put(treeNumber, hashCodes);
        }
    }


    public boolean existsOOBtuple(DataTuple tuple) {
        boolean exists = false;
        boolean existsInUsage = existsInOOBUsage(tuple); // m_OOBUsage.containsKey(tuple.hashCode())
        boolean existsInPred = existsInOOBPredictions(tuple); // m_OOBPredictions.containsKey(tuple.hashCode())
        if (existsInUsage && existsInPred)
            exists = true;
        if (!existsInUsage && existsInPred)
            System.err.println(this.getClass().getName() + ":existsOOBtuple(DataTuple) OOB tuples mismatch-> Usage = False, Predictions = True");
        if (existsInUsage && !existsInPred)
            System.err.println(this.getClass().getName() + ":existsOOBtuple(DataTuple) OOB tuples mismatch-> Usage = True, Predictions = False");
        return exists;
    }


    public void addOOBTuple(DataTuple tuple, ClusModel model) {
        putToOOBUsage(tuple, 1); // m_OOBUsage.put(tuple.hashCode(), 1);

        ClusStatistic stat = model.predictWeighted(tuple);

        switch (m_Mode) {
            case ClusStatManager.MODE_HIERARCHICAL:
                // for HMC we store the averages
                put1DArrayToOOBPredictions(tuple, ((WHTDStatistic) stat).getNumericPred());// m_OOBPredictions.put(tuple.hashCode(),stat.getNumericPred());
                break;

            case ClusStatManager.MODE_REGRESSION:
                // for Regression we store the averages
                put1DArrayToOOBPredictions(tuple, ((RegressionStat) stat).getNumericPred());// m_OOBPredictions.put(tuple.hashCode(),
                // stat.getNumericPred());
                break;

            case ClusStatManager.MODE_CLASSIFY:
                // this should have a [][].for each attribute we store: Majority: the winning class, for Probability
                // distribution, the class distribution

                if (getSettings().getEnsemble().getClassificationVoteType() == SettingsEnsemble.VOTING_TYPE_PROBAB_DISTR) {
                    // m_OOBPredictions.put(tuple.hashCode(),
                    // ClusEnsembleInduceOptimization.transformToProbabilityDistribution(stat.m_ClassCounts));
                    put2DArrayToOOBPredictions(tuple, ClusEnsembleInduceOptimization.transformToProbabilityDistribution(((ClassificationStat) stat).m_ClassCounts));
                }
                else {
                    // default is Majority Vote
                    // m_OOBPredictions.put(tuple.hashCode(),
                    // ClusEnsembleInduceOptimization.transformToMajority(stat.m_ClassCounts));
                    put2DArrayToOOBPredictions(tuple, ClusEnsembleInduceOptimization.transformToMajority(((ClassificationStat) stat).m_ClassCounts));//
                }
                break;
        }

        //store votes (we need this for automatic selection of threshold for SSL self-training)
        //FIXME: call of static Settings, should be refactored to avoid this 
        if (getSettings().getSSL().getUnlabeledCriteria() == SettingsSSL.SSL_UNLABELED_CRITERIA_AUTOMATICOOB
                || getSettings().getSSL().getUnlabeledCriteria() == SettingsSSL.SSL_UNLABELED_CRITERIA_AUTOMATICOOBINITIAL) {
            ArrayList votes = new ArrayList();
            votes.add(stat);
            m_OOBVotes.put(tuple.hashCode(), votes);
        }
    }


    public void updateOOBTuple(DataTuple tuple, ClusModel model) {
        Integer used = getFromOOBUsage(tuple); // m_OOBUsage.get(tuple.hashCode());
        used = used.intValue() + 1;
        putToOOBUsage(tuple, used); // m_OOBUsage.put(tuple.hashCode(), used);
        ClusStatistic stat = model.predictWeighted(tuple);
        double[] predictions, avg_predictions;

        switch (m_Mode) {
            case ClusStatManager.MODE_HIERARCHICAL:
                // the HMC and Regression have the same voting scheme: average
                predictions = ((WHTDStatistic) stat).getNumericPred();
                avg_predictions = get1DArrayFromOOBPredictions(tuple); // (double[])m_OOBPredictions.get(tuple.hashCode());
                avg_predictions = ClusEnsembleInduceOptimization.incrementPredictions(avg_predictions, predictions, used.doubleValue());
                put1DArrayToOOBPredictions(tuple, avg_predictions); // m_OOBPredictions.put(tuple.hashCode(),
                                                                    // avg_predictions);
                break;

            case ClusStatManager.MODE_REGRESSION:
                // the HMC and Regression have the same voting scheme: average
                predictions = ((RegressionStat) stat).getNumericPred();
                avg_predictions = get1DArrayFromOOBPredictions(tuple); // (double[])m_OOBPredictions.get(tuple.hashCode());
                avg_predictions = ClusEnsembleInduceOptimization.incrementPredictions(avg_predictions, predictions, used.doubleValue());
                put1DArrayToOOBPredictions(tuple, avg_predictions); // m_OOBPredictions.put(tuple.hashCode(),
                                                                    // avg_predictions);

                break;
            case ClusStatManager.MODE_CLASSIFY:
                // implement just addition!!!! and then
                ClassificationStat statc = (ClassificationStat) stat;
                double[][] preds = statc.m_ClassCounts.clone();

                if (getSettings().getEnsemble().getClassificationVoteType() == SettingsEnsemble.VOTING_TYPE_PROBAB_DISTR) {
                    preds = ClusEnsembleInduceOptimization.transformToProbabilityDistribution(preds);
                }
                else {
                    // default is Majority Vote
                    preds = ClusEnsembleInduceOptimization.transformToMajority(preds);
                }
               
                double[][] sum_predictions = get2DArrayFromOOBPredictions(tuple); // (double[][])m_OOBPredictions.get(tuple.hashCode());
                sum_predictions = ClusEnsembleInduceOptimization.incrementPredictions(sum_predictions, preds);
                put2DArrayToOOBPredictions(tuple, sum_predictions);// m_OOBPredictions.put(tuple.hashCode(),
                                                                   // sum_predictions);
                break;
        }

        //store votes (we need this for automatic selection of threshold for SSL self-training)
        //FIXME: call of static Settings, should be refactored to avoid this
        if (getSettings().getSSL().getUnlabeledCriteria() == SettingsSSL.SSL_UNLABELED_CRITERIA_AUTOMATICOOB 
                || getSettings().getSSL().getUnlabeledCriteria() == SettingsSSL.SSL_UNLABELED_CRITERIA_AUTOMATICOOBINITIAL) {
            ((ArrayList) m_OOBVotes.get(tuple.hashCode())).add(stat);
        }
    }


    public final void calcOOBError(OOBSelection oob_tot, RowData all_data, int type, ClusRun cr) throws IOException, ClusException {
        ClusSchema mschema = all_data.getSchema();
        // if (iter.shouldAttach()) attachModels(mschema, cr);
        cr.initModelProcessors(type, mschema);
        ModelProcessorCollection allcoll = cr.getAllModelsMI().getAddModelProcessors(type);
        DataTuple tuple;// = iter.readTuple();

        for (int t = 0; t < all_data.getNbRows(); t++) {
            if (oob_tot.isSelected(t)) {
                tuple = all_data.getTuple(t);
                allcoll.exampleUpdate(tuple);
                for (int i = 0; i < cr.getNbModels(); i++) {
                    ClusModelInfo mi = cr.getModelInfo(i);
                    ClusModel model = mi.getModel();
                    if (model != null) {
                        ClusStatistic pred = model.predictWeighted(tuple);
                        ClusErrorList err = mi.getError(type);
                        if (err != null)
                            err.addExample(tuple, pred);
                        ModelProcessorCollection coll = mi.getModelProcessors(type);
                        if (coll != null) {
                            if (coll.needsModelUpdate()) {
                                model.applyModelProcessors(tuple, coll);
                                coll.modelDone();
                            }
                            coll.exampleUpdate(tuple, pred);
                        }
                    }
                }
                allcoll.exampleDone();
            }
        }
        cr.termModelProcessors(type);
    }


    /**
     * Returns true is a given tuple is OOB for a tree in random forest with a
     * specified number
     *
     * @param treeNumber
     * @return
     */
    public static boolean isOOBForTree(DataTuple tuple, int treeNumber) {
        boolean isOOB;
        m_LockUsage.readingLock();
        if (!OOBMapping.containsKey(treeNumber)) {
            isOOB = false;
            m_LockUsage.readingUnlock();
            return isOOB;
        }

        isOOB = ((ArrayList) OOBMapping.get(treeNumber)).contains(tuple.hashCode());
        m_LockUsage.readingUnlock();
        return isOOB;
    }

    // NONSTATIC GETTERS, SETTERS and 'CHECKERS' for


    // OOBCalculation

    public static boolean isOOBCalculation() {
        m_LockCalculation.readingLock();
        boolean isCalc = m_OOBCalculation;
        m_LockCalculation.readingUnlock();
        return isCalc;
    }


    public void setOOBCalculation(boolean value) {
        m_LockCalculation.writingLock();
        m_OOBCalculation = value;
        m_LockCalculation.writingUnlock();
    }


    // OOBPredictions

    private boolean existsInOOBPredictions(DataTuple tuple) {
        m_LockPredictions.readingLock();
        boolean exists = m_OOBPredictions.containsKey(tuple.hashCode());
        m_LockPredictions.readingUnlock();
        return exists;
    }


    public void put1DArrayToOOBPredictions(DataTuple tuple, double[] value) {
        m_LockPredictions.writingLock();
        m_OOBPredictions.put(tuple.hashCode(), value);
        m_LockPredictions.writingUnlock();
    }


    public void put2DArrayToOOBPredictions(DataTuple tuple, double[][] value) {
        m_LockPredictions.writingLock();
        m_OOBPredictions.put(tuple.hashCode(), value);
        m_LockPredictions.writingUnlock();
    }


    private double[] get1DArrayFromOOBPredictions(DataTuple tuple) {
        m_LockPredictions.readingLock();
        double[] pred = (double[]) m_OOBPredictions.get(tuple.hashCode());
        double[] predictions = Arrays.copyOf(pred, pred.length);
        m_LockPredictions.readingUnlock();
        return predictions;
    }


    private double[][] get2DArrayFromOOBPredictions(DataTuple tuple) {
        m_LockPredictions.readingLock();
        double[][] pred = (double[][]) m_OOBPredictions.get(tuple.hashCode());
        double[][] predictions = new double[pred.length][];
        for (int i = 0; i < pred.length; i++) {
            predictions[i] = Arrays.copyOf(pred[i], pred[i].length);
        }
        m_LockPredictions.readingUnlock();
        return predictions;
    }


    // OOBUsage

    private boolean existsInOOBUsage(DataTuple tuple) {
        m_LockUsage.readingLock();
        boolean exists = m_OOBUsage.containsKey(tuple.hashCode());
        m_LockUsage.readingUnlock();
        return exists;
    }


    private void putToOOBUsage(DataTuple tuple, int i) {
        m_LockUsage.writingLock();
        m_OOBUsage.put(tuple.hashCode(), i);
        m_LockUsage.writingUnlock();
    }


    public Integer getFromOOBUsage(DataTuple tuple) {
        m_LockUsage.readingLock();
        Integer i = m_OOBUsage.get(tuple.hashCode());
        m_LockUsage.readingUnlock();
        return i;
    }

}