package clus.ext.semisupervised.confidence.aggregation;

import clus.ext.semisupervised.Helper;

/**
 * Averages per-target reliability scores
 * @author jurical
 */
public class Average implements Aggregation {

    @Override
    public double aggregate(double[] perTargetScores) {
        return Helper.average(perTargetScores);
    }
   
}
