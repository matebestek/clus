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

package si.ijs.kt.clus.util.format;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import si.ijs.kt.clus.util.jeans.util.StringUtils;


public class ClusFormat {

    public final static NumberFormat ONE_AFTER_DOT = makeNAfterDot(1);
    public final static NumberFormat TWO_AFTER_DOT = makeNAfterDot(2);
    public final static NumberFormat THREE_AFTER_DOT = makeNAfterDot(3);
    public final static NumberFormat MM3_AFTER_DOT = makeNAfterDot2(3);
    public final static NumberFormat SIX_AFTER_DOT = makeNAfterDot(6);
    public final static NumberFormat FOUR_AFTER_DOT = makeNAfterDot(4);
    public final static PrintWriter OUT_WRITER = new PrintWriter(new OutputStreamWriter(System.out));


    public static NumberFormat makeNAfterDot(int n) {
    	// significant places: name is for now misleading not to mess with everything ...
    	String pattern = String.format("#.%sE0", StringUtils.makeString('#', n));
    	DecimalFormat df = new DecimalFormat(pattern);
    	return df;
    }
    
    public static NumberFormat makeNAfterDotWithBug(int n) { // See issue #65
        NumberFormat fr = NumberFormat.getInstance();
        fr.setMaximumFractionDigits(n);
        try {
            DecimalFormat df = (DecimalFormat) fr;
            DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
            sym.setDecimalSeparator('.');
            df.setGroupingUsed(false);
            df.setDecimalFormatSymbols(sym);
        }
        catch (ClassCastException e) {}
        return fr;
    }


    public static NumberFormat makeNAfterDot2(int n) {
        NumberFormat fr = makeNAfterDot(n);
        fr.setMinimumFractionDigits(n);
        return fr;
    }


    public static void printArray(PrintWriter out, double[] a1, double[] a2, NumberFormat nf) {
        for (int i = 0; i < a1.length; i++) {
            if (i != 0)
                out.print(", ");
            if (a2[i] == 0.0)
                out.print(nf.format(0.0));
            else
                out.print(nf.format(a1[i] / a2[i]));
        }
    }


    public static void printArray(PrintWriter out, double[] a1, NumberFormat nf) {
        for (int i = 0; i < a1.length; i++) {
            if (i != 0)
                out.print(", ");
            out.print(nf.format(a1[i]));
        }
    }
    // TODO: move this to unit tests in the next commit 
    public static void main(String[] args) {
    	int n = 2;
    	DecimalFormat df = new DecimalFormat(String.format("#.%sE0", StringUtils.makeString('#', n)));
    	double[] xs = new double[] {9, 9.0, 9876.6, 0.0098766, 0.0000000098766};
    	String[] xsStr = new String[] {"9", "9.0", "9876.6", "0.0098766", "0.0000000098766"};
    	for(int i = 0; i < xs.length; i++) {
    		System.out.println("Representation of " + xsStr[i] + ": " + df.format(xs[i]));
    	}
    }
}
