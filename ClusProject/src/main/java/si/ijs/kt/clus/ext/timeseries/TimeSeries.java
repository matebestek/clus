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

package si.ijs.kt.clus.ext.timeseries;

import java.io.Serializable;
import java.util.StringTokenizer;

import si.ijs.kt.clus.main.settings.Settings;
import si.ijs.kt.clus.util.ClusUtil;
import si.ijs.kt.clus.util.format.ClusFormat;
import si.ijs.kt.clus.util.format.ClusNumberFormat;


public class TimeSeries implements Serializable {

    public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

    private double[] m_Values;
    private double m_TSWeight;


    public TimeSeries(String values) {
        values = values.trim();
        values = values.replace("[", "");
        values = values.replace("]", "");
        // values = values.replaceAll("\\[", "");
        // values = values.replaceAll("\\]", "");
        StringTokenizer st = new StringTokenizer(values, ",");
        m_Values = new double[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            m_Values[i++] = Double.parseDouble(st.nextToken());
        }
    }


    public TimeSeries(double[] values) {
        m_Values = new double[values.length];
        System.arraycopy(values, 0, m_Values, 0, values.length);
    }


    public TimeSeries(int size) {
        m_Values = new double[size];
        for (int i = 0; i < size; i++) {
            m_Values[i] = 0.0;
        }
    }


    public TimeSeries(TimeSeries series) {
        this(series.getValues());
    }


    public int length() {
        if (m_Values == null)
            return 0;
        return m_Values.length;
    }


    public double[] getValues() {
        double[] result = new double[m_Values.length];
        System.arraycopy(m_Values, 0, result, 0, m_Values.length);
        return result;
    }


    public double[] getValuesNoCopy() {
        return m_Values;
    }


    /*
     * [Aco]
     * Geting a single value
     */
    public double getValue(int index) {
        return m_Values[index];
    }


    /**
     * Sets a new size for the time series
     */
    public void setSize(int size) {
        m_Values = new double[size];
    }


    /*
     * [Aco]
     * resizing a time series, for any reason
     * the series length must be at least one for this to work
     */
    public void resize(int newSize, String method) {
        double[] oldValues = getValues();
        int oldSize = length();
        double[] values = new double[newSize];
        int tmpOriginal;
        double w;
        double precision = 0.00000001;
        if (method.compareTo("linear") == 0) {
            for (int i = 0; i < newSize; i++) {
                tmpOriginal = (int) Math.floor(i * ((float) oldSize / (float) newSize) + precision);
                w = i * ((float) oldSize / (float) newSize) - tmpOriginal;
                if (Math.abs(w) < precision) {
                    values[i] = oldValues[tmpOriginal];
                }
                else {
                    values[i] = oldValues[tmpOriginal] * (1 - w) + w * oldValues[tmpOriginal + 1];
                }
            }
        }
    }


    /*
     * [Aco]
     * rescaling a time series, for any reason
     * the series length must be at least one for this to work
     */
    public void rescale(double min, double max) {
        double tmpMin = min();
        double tmpMax = max();
        if (tmpMax == tmpMin)
            for (int i = 0; i < length(); i++)
                m_Values[i] = (max - min) / 2;
        else
            for (int i = 0; i < length(); i++)
                m_Values[i] = ((m_Values[i] - tmpMin) / (tmpMax - tmpMin)) * (max - min) + min;
    }


    /*
     * [Aco]
     * minimal element
     */
    public double min() {
        double r = Double.POSITIVE_INFINITY;
        for (int i = 0; i < length(); i++) {
            if (r > m_Values[i]) {
                r = m_Values[i];
            }
        }
        return r;
    }


    /*
     * [Aco]
     * maximal element
     */
    public double max() {
        double r = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < length(); i++) {
            if (r < m_Values[i]) {
                r = m_Values[i];
            }
        }
        return r;

    }


    public void setValues(double[] values) {
        System.arraycopy(values, 0, this.m_Values, 0, values.length);
    }


    /*
     * [Aco]
     * seting a single value
     */
    public void setValue(int index, double value) {
        m_Values[index] = value;
    }


    /*
     * [Aco]
     * For easy printing of the series
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	ClusNumberFormat fr = ClusFormat.SIX_AFTER_DOT;
        StringBuffer a = new StringBuffer("[");
        for (int i = 0; i < length() - 1; i++) {
            a.append(fr.format(m_Values[i]));
            a.append(',');
        }
        if (length() > 0)
            a.append(fr.format(m_Values[length() - 1]));
        a.append(']');
        return a.toString();
    }


    public double geTSWeight() {
        return m_TSWeight;
    }


    public void setTSWeight(double weight) {
        m_TSWeight = weight;
    }
    
    /**
     * Checks the equality of the two, possibly null time series. They are equal if they are either both null or of the same length, weight and values.
     * @author matejp
     * @param ts1
     * @param ts2

     */
    public static boolean areEqual(TimeSeries ts1, TimeSeries ts2) {
    	if(ts1 == null) {
    		return ts2 == null;
    	}
        if(ts2 == null){
            return ts1 == null;
        }
    	if(ts1.length() != ts2.length()) {
    		return false;
    	}
    	if(!ClusUtil.eq(ts1.geTSWeight(), ts2.geTSWeight(), ClusUtil.MICRO)) {
    		return false;
    	}
    	for(int i = 0; i < ts1.length(); i++) {
    		if(!ClusUtil.eq(ts1.getValue(i), ts2.getValue(i), ClusUtil.MICRO)) {
    			return false;
    		}
    	}
    	return true;
    }

}
