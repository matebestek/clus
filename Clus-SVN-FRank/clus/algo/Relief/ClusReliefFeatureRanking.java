package clus.algo.Relief;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;
import clus.data.type.NominalAttrType;
import clus.data.type.NumericAttrType;
import clus.data.type.StringAttrType;
import clus.data.type.TimeSeriesAttrType;
import clus.ext.ensembles.ClusEnsembleFeatureRanking;
import clus.ext.timeseries.DTWTimeSeriesDist;
import clus.ext.timeseries.QDMTimeSeriesDist;
import clus.ext.timeseries.TSCTimeSeriesDist;
import clus.ext.timeseries.TimeSeries;
import clus.main.Settings;
import clus.util.ClusException;

public class ClusReliefFeatureRanking extends ClusEnsembleFeatureRanking{
	private int m_NbNeighbours;
	private int m_NbIterations;

	
	private ClusAttrType[][] m_DescriptiveTargetAttr = new ClusAttrType[2][];	// {array of descriptive attributes, array of target attributes}
	
	private int m_NbDescriptiveAttrs, m_NbTargetAttrs;		// number of descriptive and target attributes, respectively
	
	
	private HashMap<String, Double> m_numMins, m_numMaxs;	// min and max for each numeric attribute
	private int m_NbExamples;								// number of examples in the data		
	double m_bothMissing = 1.0;								// distances in the case of missing values
	boolean m_isDeterministic;								// tells whether the next instance generator is deterministic (if and only if m_NbIterations == m_NbExamples)
	Random m_rnd = new Random(1234);						// random generator that is used iff not m_isDeterministic
	boolean m_isStandardClassification;						// standard classification or general case
	int m_NbTargetValues;									// number of target values: if m_isStandardClassification: self explanatory, else: = 1
	double[] m_targetProbabilities;							// relative frequencies of the target values
	
	int m_TimeSeriesDistance; 								// type of the time series distance
	
	boolean debug = false;
	

	
	public ClusReliefFeatureRanking(int neighbours, int iterations){
		super();
		this.m_NbNeighbours = neighbours;
		this.m_NbIterations = iterations;
	}
	
	public void calculateReliefImportance(RowData data) throws ClusException {
		m_TimeSeriesDistance = data.m_Schema.getSettings().m_TimeSeriesDistance.getValue();		
		setReliefDescription(m_NbNeighbours, m_NbIterations);
		m_NbExamples = data.getNbRows();
		m_isDeterministic = m_NbExamples == m_NbIterations;
		
		// initialize descriptive and target attributes if necessary
		int attrType;
		for(int space = 0; space < 2; space++){
			attrType = space == 0 ? ClusAttrType.ATTR_USE_DESCRIPTIVE : ClusAttrType.ATTR_USE_TARGET;
			if(m_DescriptiveTargetAttr[space] == null) m_DescriptiveTargetAttr[space] = data.m_Schema.getAllAttrUse(attrType);
		}
		m_NbDescriptiveAttrs = m_DescriptiveTargetAttr[0].length;
		m_NbTargetAttrs =  m_DescriptiveTargetAttr[1].length;
		m_isStandardClassification = m_NbTargetAttrs == 1 && m_DescriptiveTargetAttr[1][0] instanceof NominalAttrType;
		m_NbTargetValues = m_isStandardClassification ? ((NominalAttrType) m_DescriptiveTargetAttr[1][0]).getNbValues() : 1;
		
		// class counts
		if(m_isStandardClassification){
			m_targetProbabilities = new double[m_NbTargetValues + 1]; // one additional place for missing values
			NominalAttrType attr = (NominalAttrType) m_DescriptiveTargetAttr[1][0];
			for(int example = 0; example < m_NbExamples; example++){
				m_targetProbabilities[attr.getNominal(data.getTuple(example))] += 1.0;
			}
			if(m_NbExamples > m_targetProbabilities[m_NbTargetValues]){ // otherwise: m_TargetProbabilities = {0, 0, ... , 0, m_NbExamples}
				// normalize probabilities: examples with unknown targets are ignored
				// The formula for standard classification case still holds (sum over other classes of P(other class) / (1 - P(class)) equals 1)
				for(int value = 0; value < m_NbTargetValues; value++){
					m_targetProbabilities[value] /= m_NbExamples - m_targetProbabilities[m_NbTargetValues];
				}
			}
		}
		
		// compute min and max of numeric attributes	
		m_numMins = new HashMap<String, Double>();
		m_numMaxs = new HashMap<String, Double>();
		double value;
		String attrName;
		for(int space = 0; space < 2; space++){
			if(space == 0) attrType = ClusAttrType.ATTR_USE_DESCRIPTIVE;
			else attrType = ClusAttrType.ATTR_USE_TARGET;
			for(NumericAttrType numAttr : data.m_Schema.getNumericAttrUse(attrType)){
				attrName = numAttr.getName();
				m_numMins.put(attrName, Double.POSITIVE_INFINITY);
				m_numMaxs.put(attrName, Double.NEGATIVE_INFINITY);
				for(int example = 0; example < m_NbExamples; example++){
					value = numAttr.getNumeric(data.getTuple(example));
					if(value < m_numMins.get(attrName)){ // equivalent to ... && value != Double.POSITIVE_INFINITY
						m_numMins.put(attrName, value);
					}
					if(value > m_numMaxs.get(attrName) && value != Double.POSITIVE_INFINITY){
						m_numMaxs.put(attrName, value);
					}
				}
			}			
		}
		System.out.println("min: " + m_numMins);
		System.out.println("max: " + m_numMaxs);
		
		System.out.println("attrs: " + Arrays.deepToString(m_DescriptiveTargetAttr));

		// attribute relevance
		double[] sumDistAttr = new double[m_NbDescriptiveAttrs];
		double sumDistTarget = 0.0;
		double[] sumDistAttrTarget = new double[m_NbDescriptiveAttrs];
		DataTuple tuple;
		int tupleInd;
		NearestNeighbour[][] nearestNeighbours;
		ClusAttrType attr;
		for(int iteration = 0; iteration < m_NbIterations; iteration++){
			// CHOOSE TUPLE AND COMPUTE NEAREST NEIGHBOURS
			tupleInd = nextInstance(iteration);
			tuple = data.getTuple(tupleInd);
			if(debug)System.out.println("Tuple: " + tuple.toString());
			if(!(m_isStandardClassification && m_DescriptiveTargetAttr[1][0].isMissing(tuple))){
				nearestNeighbours = findNearestNeighbours(tupleInd, data);
				// CALCULATE IMPORTANCES
				for(int targetValue = 0; targetValue < m_NbTargetValues; targetValue++){
					double tempSumDistTarget = 0.0;
					double[] tempSumDistAttr = new double[m_NbDescriptiveAttrs];
					double[] tempSumDistAttrTarget = new double[m_NbDescriptiveAttrs];
					for(int neighbour = 0; neighbour < nearestNeighbours[targetValue].length; neighbour++){
						if(!m_isStandardClassification){
							tempSumDistTarget += nearestNeighbours[targetValue][neighbour].m_targetDistance;
						}
						for(int attrInd = 0; attrInd < m_NbDescriptiveAttrs; attrInd++){
							attr = m_DescriptiveTargetAttr[0][attrInd];
							double distAttr = calcDistance1D(tuple, data.getTuple(nearestNeighbours[targetValue][neighbour].m_indexInDataSet), attr);						
							if(m_isStandardClassification){
								int tupleTarget = ((NominalAttrType) m_DescriptiveTargetAttr[1][0]).getNominal(tuple);
								tempSumDistAttr[attrInd] += targetValue == tupleTarget ? -distAttr: m_targetProbabilities[targetValue] / (1.0 - m_targetProbabilities[tupleTarget]) * distAttr;							
							} else{							
								tempSumDistAttr[attrInd] += distAttr;
								tempSumDistAttrTarget[attrInd] += distAttr * nearestNeighbours[targetValue][neighbour].m_targetDistance;
							}	
						}
					}
					sumDistTarget += tempSumDistTarget / nearestNeighbours[targetValue].length;
					for(int attrInd = 0; attrInd < m_NbDescriptiveAttrs; attrInd++){
						sumDistAttr[attrInd] += tempSumDistAttr[attrInd] / nearestNeighbours[targetValue].length;
						sumDistAttrTarget[attrInd] += tempSumDistAttrTarget[attrInd] /  nearestNeighbours[targetValue].length;
					}
				}
			}
		}
		// UPDATE IMPORTANCES
		for(int attrInd = 0; attrInd < m_NbDescriptiveAttrs; attrInd++){
			attr = m_DescriptiveTargetAttr[0][attrInd];
			double [] info = getAttributeInfo(attr.getName());
			if(m_isStandardClassification){
				info[2] += sumDistAttr[attrInd] / m_NbIterations;
			} else{
				info[2] += sumDistAttrTarget[attrInd] / sumDistTarget - (sumDistAttr[attrInd] - sumDistAttrTarget[attrInd]) / (m_NbIterations - sumDistTarget);
			}
			putAttributeInfo(attr.getName(), info);
		}		
	}
	
	/**
	 * Computes the nearest neighbours of example with index {@code tupleInd} in the dataset {@code data}.
	 * @param tupleInd
	 * @param data
	 * @return
	 * @throws ClusException
	 */
	public NearestNeighbour[][] findNearestNeighbours(int tupleInd, RowData data) throws ClusException{
		DataTuple tuple = data.getTuple(tupleInd);
		int[][] neighbours = new int[m_NbTargetValues][m_NbNeighbours]; 		// current candidates
		double[] distances = new double[m_NbExamples];							// distances[i] = distance(tuple, data.getTuple(i))	TODO: mal prostora porabmo, ce je pdoatk. mnozica stevilcna ...
		int[] whereToPlaceNeigh = new int[m_NbTargetValues];
		int targetValue;

		for(int i = 0; i < m_NbExamples;i++){
			distances[i] = calcDistance(tuple, data.getTuple(i), 0); // in descriptive space
		}
		if(debug){
			System.out.println("  scores: " + Arrays.toString(distances));
		}
		boolean sortingNeeded;
	    for (int i = 0; i < m_NbExamples; i++){
	    	sortingNeeded = false;
	        if(i != tupleInd){
	        	targetValue = m_isStandardClassification ? m_DescriptiveTargetAttr[1][0].getNominal(data.getTuple(i)) : 0;
      		
        		if(targetValue < m_NbTargetValues){ // non-missing
        			if (whereToPlaceNeigh[targetValue] < m_NbNeighbours){
	        			neighbours[targetValue][whereToPlaceNeigh[targetValue]] = i;
	        			whereToPlaceNeigh[targetValue]++;
	        			if(whereToPlaceNeigh[targetValue] == m_NbNeighbours){ // the list of neighbours has just became full ---> sort it
	        			    for (int ind1 = 0; ind1 < m_NbNeighbours; ind1++) { // spremeni, ce je NbNeighbours velka stvar ...
	        			        for (int ind2 = ind1 + 1; ind2 < m_NbNeighbours; ind2++) {
	        			            if (distances[neighbours[targetValue][ind1]] < distances[neighbours[targetValue][ind2]]) {
	        			                int temp = neighbours[targetValue][ind1];
	        			                neighbours[targetValue][ind1] = neighbours[targetValue][ind2];
	        			                neighbours[targetValue][ind2] = temp;
	        			            }	
	        			        }
	        			    }
	        				
	        			}
	        			if(debug) System.out.println("    after first nbNeigh: " + Arrays.toString(neighbours[targetValue]));	        			
        			} else{
        				sortingNeeded = true;
        			}
        		} else{
        			// nothing to do here
        		}        		
        		if(sortingNeeded){
    		        if (distances[i] >= distances[neighbours[targetValue][0]]) {
    		            continue;
    		        }
    		        int j; // here the branch prediction should kick-in
    		        for (j = 1; j < m_NbNeighbours && distances[i] < distances[neighbours[targetValue][j]]; j++) {
    		            neighbours[targetValue][j - 1] = neighbours[targetValue][j];
    		        }
    		        neighbours[targetValue][j - 1] = i;
    		        if(debug) System.out.println("    after additional sorting: " + Arrays.toString(neighbours[targetValue]));	
        		}

	        }
	    }
		if(debug){
			System.out.println("   nearest: " + Arrays.deepToString(neighbours));
			System.out.println();
		}
		NearestNeighbour[][] nearestNeighbours = new NearestNeighbour[m_NbTargetValues][];
		for(int value = 0; value < m_NbTargetValues; value++){
			nearestNeighbours[value] = new NearestNeighbour[whereToPlaceNeigh[value]];
			for(int i = 0; i < whereToPlaceNeigh[value]; i++){
				nearestNeighbours[value][i] = new NearestNeighbour(neighbours[value][i], distances[neighbours[value][i]], calcDistance(tuple, data.getTuple(neighbours[value][i]), 1));
			}
		}
		return nearestNeighbours;
	}
	
	/**
	 * Distance between tuples in the subspace {@code space}.
	 * @param t1
	 * @param t2
	 * @param space if 0, subspace is descriptive space, else target space
	 * @return
	 * @throws ClusException 
	 */
    public double calcDistance(DataTuple t1, DataTuple t2, int space) throws ClusException {
        double dist = 0.0;
        int dimensions = space == 0 ? m_NbDescriptiveAttrs : m_NbTargetAttrs;
        ClusAttrType attr;
    	for(int attrInd = 0; attrInd < dimensions; attrInd++){
    		attr = m_DescriptiveTargetAttr[space][attrInd];
    		dist += calcDistance1D(t1, t2, attr);
    	}
        return dist / dimensions;
    }
    /**
     * Calculates the distance between to tuples in a given component {@code attr}. 
     * @param t1
     * @param t2
     * @param attr
     * @return
     * @throws ClusException 
     */
    public double calcDistance1D(DataTuple t1, DataTuple t2, ClusAttrType attr) throws ClusException{
		if(attr instanceof NominalAttrType){
			return calculateNominalDist1D(t1, t2, (NominalAttrType) attr);
		} else if(attr instanceof NumericAttrType){
			double normFactor = m_numMaxs.get(attr.getName()) - m_numMins.get(attr.getName());
			if(normFactor == 0.0){ // if and only if the attribute has only one value ... Distance will be zero and does not depend on normFactor
				normFactor = 1.0;
			}
			return calculateNumericDist1D(t1, t2, (NumericAttrType) attr, normFactor);
		} else if(attr instanceof TimeSeriesAttrType) {
			return calculateTimeSeriesDist1D(t1, t2, (TimeSeriesAttrType) attr);
		} else if(attr instanceof StringAttrType){
			return calculateStringDist1D(t1, t2, (StringAttrType) attr);
		} else{
			throw new ClusException("Unknown attribute type for attribute " + attr.getName());
		}
    	
    }
	
    // TODO: make handling of missing values reliefish for all dist.
    /**
     * Calculates distance between the nominal values of the component {@code attr}. In the case of missing values, we follow Weka's solution
     * and not the paper Theoretical and Empirical Analysis of ReliefF and RReliefF, by Robnik Sikonja and Kononenko (time complexity ...).
     * @param t1
     * @param t2
     * @param attr
     * @return
     */
    public double calculateNominalDist1D(DataTuple t1, DataTuple t2, NominalAttrType attr){
		int v1 = attr.getNominal(t1);
		int v2 = attr.getNominal(t2);
		if(v1 >= attr.m_NbValues || v2 >= attr.m_NbValues){ // at least one missing
			return 1.0 - 1.0 / attr.m_NbValues;
		} else{
			return v1 == v2 ? 0.0 : 1.0;
		}
    }
    
    /**
     * Calculates distance between the numeric values of the component {@code attr}. In the case of missing values, we follow Weka's solution
     * and not the paper Theoretical and Empirical Analysis of ReliefF and RReliefF, by Robnik Sikonja and Kononenko (time complexity ...).
     * @param t1
     * @param t2
     * @param attr
     * @param normalizationFactor
     * @return
     */
    public double calculateNumericDist1D(DataTuple t1, DataTuple t2, NumericAttrType attr, double normalizationFactor){
		double v1 = attr.getNumeric(t1);
		double v2 = attr.getNumeric(t2);
		double t;
		if(t1.hasNumMissing(attr.getArrayIndex())){
			if(t2.hasNumMissing(attr.getArrayIndex())){
				t = m_bothMissing;
			} else{
				t = (v2 - m_numMins.get(attr.getName())) / normalizationFactor;
				t = Math.max(t, 1.0 - t);
			}
		} else{
			if(t2.hasNumMissing(attr.getArrayIndex())){
				t = (v1 - m_numMins.get(attr.getName())) / normalizationFactor;
				t = Math.max(t, 1.0 - t);
			} else{
				t =  Math.abs(v1 - v2) / normalizationFactor;
			}
		}
		return t;
    	
    }
    
    /**
     * Computes distance between the time series values of the component {@code attr}.
     * @param t1
     * @param t2
     * @param attr
     * @return
     * @throws ClusException 
     */
    public double calculateTimeSeriesDist1D(DataTuple t1, DataTuple t2, TimeSeriesAttrType attr) throws ClusException{
    	TimeSeries ts1 = attr.getTimeSeries(t1);
    	TimeSeries ts2 = attr.getTimeSeries(t2);
    	
		switch (m_TimeSeriesDistance) {
		case Settings.TIME_SERIES_DISTANCE_MEASURE_DTW:
			return new DTWTimeSeriesDist(attr).calcDistance(t1, t2);
		case Settings.TIME_SERIES_DISTANCE_MEASURE_QDM:
			if (ts1.length() == ts2.length()) {
				return new QDMTimeSeriesDist(attr).calcDistance(t1, t2);
			} else {
				throw new ClusException("QDM Distance is not implemented for time series with different length");
			}
		case Settings.TIME_SERIES_DISTANCE_MEASURE_TSC:
			return new TSCTimeSeriesDist(attr).calcDistance(t1, t2);
		default:
			throw new ClusException("ClusReliefFeatureRanking.m_TimeSeriesDistance was not set to any known value.");
		}    	
    	
    }
    
    /**
     * Computes Levenshtein's distance between the string values of the component {@code attr}.
     * @param t1
     * @param t2
     * @param attr
     * @return
     */
    public double calculateStringDist1D(DataTuple t1, DataTuple t2, StringAttrType attr){
    	return new Levenshtein(t1, t2, attr).getDist();    	
    }
    
	public void sortFeatureRanks(){
		Iterator iter = m_AllAttributes.keySet().iterator();
		while (iter.hasNext()){
			String attr = (String)iter.next();
//			double score = ((double[])m_AllAttributes.get(attr))[2]/ClusEnsembleInduce.getMaxNbBags();
			double score = ((double[])m_AllAttributes.get(attr))[2];
			ArrayList attrs = new ArrayList();
			if (m_FeatureRanks.containsKey(score))
				attrs = (ArrayList)m_FeatureRanks.get(score);
			attrs.add(attr);
			m_FeatureRanks.put(score, attrs);
		}
	}
	
	/**
	 * Returns the index of the chosen example in the iteration {@code iteration}.
	 * @param iteration
	 * @return
	 */
	private int nextInstance(int iteration){
		if(m_isDeterministic){
			return iteration;
		} else{
			return (int) (m_rnd.nextDouble() * m_NbExamples);
		}
	}
}
