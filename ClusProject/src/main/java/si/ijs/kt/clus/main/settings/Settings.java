/*************************************************************************
 * Clus - Software for Predictive Clustering *
 * Copyright (C) 2007 *
 * Katholieke Universiteit Leuven, Leuven, Belgium *
 * Jozef Stefan Institute, Ljubljana, Slovenia *
 * *
 * This program is free software: you can redistribute it and/or modify *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation, either version 3 of the License, or *
 * (at your option) any later version. *
 * *
 * This program is distributed in the hope that it will be useful, *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. *
 * *
 * You should have received a copy of the GNU General Public License *
 * along with this program. If not, see <http://www.gnu.org/licenses/>. *
 * *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>. *
 *************************************************************************/

package si.ijs.kt.clus.main.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import si.ijs.kt.clus.data.ClusSchema;
import si.ijs.kt.clus.data.type.primitive.IntegerAttrType;
import si.ijs.kt.clus.main.settings.section.SettingsAttribute;
import si.ijs.kt.clus.main.settings.section.SettingsBeamSearch;
import si.ijs.kt.clus.main.settings.section.SettingsConstraints;
import si.ijs.kt.clus.main.settings.section.SettingsData;
import si.ijs.kt.clus.main.settings.section.SettingsDistances;
import si.ijs.kt.clus.main.settings.section.SettingsEnsemble;
import si.ijs.kt.clus.main.settings.section.SettingsExhaustiveSearch;
import si.ijs.kt.clus.main.settings.section.SettingsExperimental;
import si.ijs.kt.clus.main.settings.section.SettingsGeneral;
import si.ijs.kt.clus.main.settings.section.SettingsGeneric;
import si.ijs.kt.clus.main.settings.section.SettingsHMLC;
import si.ijs.kt.clus.main.settings.section.SettingsHMTR;
import si.ijs.kt.clus.main.settings.section.SettingsILevelC;
import si.ijs.kt.clus.main.settings.section.SettingsKNN;
import si.ijs.kt.clus.main.settings.section.SettingsKNNTree;
import si.ijs.kt.clus.main.settings.section.SettingsMLC;
import si.ijs.kt.clus.main.settings.section.SettingsModel;
import si.ijs.kt.clus.main.settings.section.SettingsNominal;
import si.ijs.kt.clus.main.settings.section.SettingsOptionTree;
import si.ijs.kt.clus.main.settings.section.SettingsOutput;
import si.ijs.kt.clus.main.settings.section.SettingsPhylogeny;
import si.ijs.kt.clus.main.settings.section.SettingsRelief;
import si.ijs.kt.clus.main.settings.section.SettingsRules;
import si.ijs.kt.clus.main.settings.section.SettingsSIT;
import si.ijs.kt.clus.main.settings.section.SettingsSSL;
import si.ijs.kt.clus.main.settings.section.SettingsTimeSeries;
import si.ijs.kt.clus.main.settings.section.SettingsTree;
import si.ijs.kt.clus.util.jeans.io.ini.INIFile;
import si.ijs.kt.clus.util.jeans.resource.ResourceInfo;
import si.ijs.kt.clus.util.jeans.util.StringUtils;
import si.ijs.kt.clus.util.jeans.util.cmdline.CMDLineArgs;


/**
 * All the settings. Includes the command line parameters as boolean class attributes.
 * The settings file attributes are included by get* methods.
 * 
 * @author User
 *
 */
public class Settings implements Serializable {

    public final static long SERIAL_VERSION_ID = 1L;
    public final static long serialVersionUID = SERIAL_VERSION_ID;

    /***********************************************************************
     * Create the settings structure *
     ***********************************************************************/
    private INIFile m_Ini; // main INI file structure
    private ArrayList<SettingsBase> m_Sections; // list of all settings classes

    /**
     * Individual settings classes
     */
    private SettingsGeneric m_SettGeneric;
    private SettingsGeneral m_SettGeneral;
    private SettingsData m_SettData;
    private SettingsAttribute m_SettAttribute;
    private SettingsConstraints m_SettConstraints;
    private SettingsOutput m_SettOutput;
    private SettingsNominal m_SettNominal;
    private SettingsModel m_SettModel;
    private SettingsTree m_SettTree;
    private SettingsRules m_SettRules;
    private SettingsMLC m_SettMLC;
    private SettingsHMLC m_SettHMLC;
    private SettingsHMTR m_SettHMTR;
    private SettingsILevelC m_SettILevelC;
    private SettingsBeamSearch m_SettBeamSearch;
    private SettingsExhaustiveSearch m_SettExhaustiveSearch;
    private SettingsTimeSeries m_SettTimeSeries;
    private SettingsPhylogeny m_SettPhylogeny;
    private SettingsRelief m_SettRelief;
    private SettingsDistances m_SettDistances;
    private SettingsEnsemble m_SettEnsemble;
    private SettingsKNN m_SettKNN;
    private SettingsKNNTree m_SettKNNTree;
    private SettingsOptionTree m_SettOptionTree;
    private SettingsExperimental m_SettExperimental;
    private SettingsSIT m_SettSIT;
    private SettingsSSL m_SettSSL;


    public Settings() {
        m_Ini = new INIFile();
        m_Sections = new ArrayList<SettingsBase>();

        // Initialize individual settings. Order of initialization is important (see dependencies in the constructors).
        m_SettOutput = new SettingsOutput(27);
        m_SettGeneric = new SettingsGeneric(m_SettOutput);
        m_SettGeneral = new SettingsGeneral(1);
        m_SettData = new SettingsData(2);
        m_SettAttribute = new SettingsAttribute(3);
        m_SettNominal = new SettingsNominal(7);
        m_SettModel = new SettingsModel(9);
        m_SettTree = new SettingsTree(10);
        m_SettRules = new SettingsRules(11);
        m_SettHMLC = new SettingsHMLC(13);
        m_SettILevelC = new SettingsILevelC(16);
        m_SettBeamSearch = new SettingsBeamSearch(17);
        m_SettExhaustiveSearch = new SettingsExhaustiveSearch(18);
        m_SettTimeSeries = new SettingsTimeSeries(8);
        m_SettPhylogeny = new SettingsPhylogeny(19);
        m_SettRelief = new SettingsRelief(20);
        m_SettDistances = new SettingsDistances(4);
        m_SettEnsemble = new SettingsEnsemble(21);
        m_SettKNN = new SettingsKNN(22);
        m_SettKNNTree = new SettingsKNNTree(23);
        m_SettOptionTree = new SettingsOptionTree(15);
        m_SettSIT = new SettingsSIT(24);
        m_SettExperimental = new SettingsExperimental(26);
        m_SettConstraints = new SettingsConstraints(5);
        m_SettMLC = new SettingsMLC(12, m_SettHMLC, m_SettRelief);
        m_SettHMTR = new SettingsHMTR(14, m_SettAttribute, m_SettGeneral);
        m_SettSSL = new SettingsSSL(25);

        // store all settings classes in m_Sections
        Collections.addAll(m_Sections,
                m_SettGeneral,
                m_SettData,
                m_SettAttribute,
                m_SettConstraints,
                m_SettOutput,
                m_SettNominal,
                m_SettModel,
                m_SettTree,
                m_SettRules,
                m_SettMLC,
                m_SettHMLC,
                m_SettHMTR,
                m_SettILevelC,
                m_SettBeamSearch,
                m_SettExhaustiveSearch,
                m_SettTimeSeries,
                m_SettPhylogeny,
                m_SettRelief,
                m_SettDistances,
                m_SettEnsemble,
                m_SettKNN,
                m_SettKNNTree,
                m_SettOptionTree,
                m_SettExperimental,
                m_SettSIT,
                m_SettSSL);

    }


    public SettingsGeneric getGeneric() {
        return m_SettGeneric;
    }


    public SettingsGeneral getGeneral() {
        return m_SettGeneral;
    }


    public SettingsData getData() {
        return m_SettData;
    }


    public SettingsAttribute getAttribute() {
        return m_SettAttribute;
    }


    public SettingsConstraints getConstraints() {
        return m_SettConstraints;
    }


    public SettingsOutput getOutput() {
        return m_SettOutput;
    }


    public SettingsNominal getNominal() {
        return m_SettNominal;
    }


    public SettingsModel getModel() {
        return m_SettModel;
    }


    public SettingsTree getTree() {
        return m_SettTree;
    }


    public SettingsRules getRules() {
        return m_SettRules;
    }


    public SettingsMLC getMLC() {
        return m_SettMLC;
    }


    public SettingsHMLC getHMLC() {
        return m_SettHMLC;
    }


    public SettingsHMTR getHMTR() {
        return m_SettHMTR;
    }


    public SettingsILevelC getILevelC() {
        return m_SettILevelC;
    }


    public SettingsBeamSearch getBeamSearch() {
        return m_SettBeamSearch;
    }


    public SettingsExhaustiveSearch getExhaustiveSearch() {
        return m_SettExhaustiveSearch;
    }


    public SettingsTimeSeries getTimeSeries() {
        return m_SettTimeSeries;
    }


    public SettingsPhylogeny getPhylogeny() {
        return m_SettPhylogeny;
    }


    public SettingsRelief getRelief() {
        return m_SettRelief;
    }


    public SettingsDistances getDistances() {
        return m_SettDistances;
    }


    public SettingsEnsemble getEnsemble() {
        return m_SettEnsemble;
    }


    public SettingsKNN getKNN() {
        return m_SettKNN;
    }


    public SettingsKNNTree getKNNTree() {
        return m_SettKNNTree;
    }


    public SettingsOptionTree getOptionTree() {
        return m_SettOptionTree;
    }


    public SettingsExperimental getExperimental() {
        return m_SettExperimental;
    }


    public SettingsSIT getSIT() {
        return m_SettSIT;
    }


    public SettingsSSL getSSL() {
        return m_SettSSL;
    }


    /**
     * This method creates INI sections and stores them in the m_Ini structure
     */
    public void create() {

        // sort the sections so that they will be listed correctly in the INI file
        Collections.sort(m_Sections, new Comparator<SettingsBase>() {

            @Override
            public int compare(SettingsBase p1, SettingsBase p2) {
                return p1.getPosition() - p2.getPosition();
            }
        });

        // create sections and add them to INI structure
        for (SettingsBase sec : m_Sections) {
            m_Ini.addNode(sec.create());
        }
    }


    public void initNamedValues() {
        for (SettingsBase sec : m_Sections) {
            sec.initNamedValues();
        }
    }


    public void updateTarget(ClusSchema schema) {
        if (m_SettTree.checkHeuristic("SSPD")) {
            schema.addAttrType(new IntegerAttrType("SSPD"));
            int nb = schema.getNbAttributes();
            m_SettAttribute.setTarget(String.valueOf(nb));
        }
    }


    public void initialize(CMDLineArgs cargs, boolean loads) throws IOException {
        create();
        initNamedValues();

        if (loads) {
            try {
                String fname = m_SettGeneric.getFileAbsolute(m_SettGeneric.getAppName() + ".s");
                m_Ini.load(fname, '%');
            }
            catch (FileNotFoundException e) {
                System.out.println("No settings file found");
            }
        }
        if (cargs != null)
            process(cargs);

        m_SettData.updateDataFile(m_SettGeneric.getAppName() + ".arff");
        m_SettHMLC.initHierarchical();
    }


    public void process(CMDLineArgs cargs) {
        if (cargs.hasOption("target")) {
            m_SettAttribute.setTarget(cargs.getOptionValue("target"));
        }
        if (cargs.hasOption("disable")) {
            String disarg = cargs.getOptionValue("disable");
            String orig = m_SettAttribute.getDisabled();

            if (StringUtils.unCaseCompare(orig, SettingsBase.NONE)) {
                m_SettAttribute.setDisabled(disarg);
            }
            else {
                m_SettAttribute.setDisabled(orig + "," + disarg);
            }
        }
        if (cargs.hasOption("silent")) {
            m_SettGeneral.enableVerbose(0);
        }
    }


    public void update(ClusSchema schema) {
        m_SettTree.setFTest(m_SettTree.getFTest(), m_SettGeneral.getVerbose());

        SettingsTree.MINIMAL_WEIGHT = m_SettModel.getMinimalWeight();
        SettingsTree.ONE_NOMINAL = (schema.getNbNominalTargetAttributes() == 1 && schema.getNbNumericTargetAttributes() == 0);
        SettingsTree.ALPHA = m_SettTree.getSpatialAlpha();

        SettingsOutput.SHOW_UNKNOWN_FREQ = m_SettOutput.isShowUnknown();
        SettingsOutput.SHOW_BRANCH_FREQ = m_SettOutput.isShowBranchFreq();

        SettingsExperimental.SHOW_XVAL_FOREST = m_SettExperimental.isShowXValForest();

        SettingsBeamSearch.SIZE_PENALTY = m_SettBeamSearch.getSizePenalty();
        SettingsBeamSearch.BEAM_WIDTH = m_SettBeamSearch.getBeamWidth();
        SettingsBeamSearch.BEAM_SIMILARITY = m_SettBeamSearch.getBeamSimilarity();
        SettingsBeamSearch.BEAM_SYNT_DIST_CONSTR = m_SettBeamSearch.hasBeamConstraintFile();
    }


    public void updateDisabledSettings() {
        int pruning = m_SettTree.getPruningMethod();
        int heur = m_SettTree.getHeuristic();

        m_SettTree.setM5PruningMultEnabled(pruning == SettingsTree.PRUNING_METHOD_M5 || pruning == SettingsTree.PRUNING_METHOD_M5_MULTI);
        m_SettTree.set1SERuleEnabled(pruning == SettingsTree.PRUNING_METHOD_GAROFALAKIS_VSB);
        m_SettTree.setFTestEnabled(heur == SettingsTree.HEURISTIC_SSPD || heur == SettingsTree.HEURISTIC_VARIANCE_REDUCTION);

        m_SettData.setPruneSetMaxEnabled(!m_SettData.isPruneSetString(SettingsBase.NONE));

        if (ResourceInfo.isLibLoaded())
            m_SettGeneral.getResourceInfoLoaded().setSingleValue(SettingsGeneral.RESOURCE_INFO_LOAD_YES);
        else
            m_SettGeneral.getResourceInfoLoaded().setSingleValue(SettingsGeneral.RESOURCE_INFO_LOAD_NO);
    }


    public void show(PrintWriter where) throws IOException {
        updateDisabledSettings();

        // For TreeToRules PredictionMethod might have been temporarily put to DecisionList instead of some other
        boolean tempInduceParamNeeded = m_SettRules.getRuleInduceParamsDisabled(); // They were changed in the first place

        if (m_SettRules.getCoveringMethod() == SettingsRules.COVERING_METHOD_RULES_FROM_TREE && tempInduceParamNeeded)
            m_SettRules.returnRuleInduceParams();

        m_Ini.save(where);

        if (m_SettRules.getCoveringMethod() == SettingsRules.COVERING_METHOD_RULES_FROM_TREE && tempInduceParamNeeded)
            m_SettRules.disableRuleInduceParams();
    }

}