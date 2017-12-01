
package si.ijs.kt.clus.error.mlcForHmlc;

import si.ijs.kt.clus.util.ClusUtil;

public class SubsetAccuracy implements MlcHmlcSubError {

	private int m_NbCorrect;
	private int m_NbKnown;

	public SubsetAccuracy() {
		m_NbCorrect = 0;
		m_NbKnown = 0;
	}

	@Override
	public double compute(int dim) {
		if (ClusUtil.isZero(m_NbKnown)) {
			return ClusUtil.ZERO;
		} else {
			return ((double) m_NbCorrect) / m_NbKnown;
		}
	}

	@Override
	public void addExample(boolean[] actual, double[] predicted, boolean[] predictedThresholded) {
		boolean correctPrediction = true;
		for (int i = 0; i < actual.length; i++) {
			if (actual[i] != predictedThresholded[i]) {
				correctPrediction = false;
				break;
			}
		}
		if (correctPrediction) {
			m_NbCorrect++;
		}
		m_NbKnown++;
	}

	@Override
	public String getName() {
		return "SubsetAccuracy";
	}

	@Override
	public void add(MlcHmlcSubError other) {
		SubsetAccuracy o = (SubsetAccuracy) other;
		m_NbCorrect += o.m_NbCorrect;
		m_NbKnown = o.m_NbKnown;
	}
}
