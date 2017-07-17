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

package clus.data.type.complex;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import clus.data.io.ClusReader;
import clus.data.rows.DataTuple;
import clus.data.type.ClusAttrType;
import clus.ext.structuredTypes.Set;
import clus.io.ClusSerializable;
import clus.main.settings.Settings;
import clus.util.ClusException;


public class SetAttrType extends ClusAttrType {

    public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

    public final static int THIS_TYPE = SET_ATR_TYPE;
    public final static String THIS_TYPE_NAME = "Set";

    private String typeDefinition;
    private int numberOfPossibleValues;

    public static boolean m_isEqualLength = true;
    int m_Length = -1;


    public SetAttrType(String name) {
        super(name);
    }


    public SetAttrType(String name, String typeDefinition) {
        super(name);
        typeDefinition = typeDefinition.toUpperCase();
        this.setTypeDefinition(typeDefinition);
        if (typeDefinition.contains("NOMINAL[")) {
            int start = typeDefinition.indexOf("NOMINAL[");
            int end = typeDefinition.indexOf("]", start);
            String nominalDefinition = typeDefinition.substring(start, end);
            StringTokenizer st = new StringTokenizer(nominalDefinition, ",");
            this.numberOfPossibleValues = st.countTokens();
        }

        //TODO implement
    }


    public SetAttrType(String name, String typeDefinition, int numberOfPossibleValues) {
        this(name, typeDefinition);
        this.setNumberOfPossibleValues(numberOfPossibleValues);
    }


    public ClusAttrType cloneType() {
        SetAttrType tsat = new SetAttrType(m_Name);
        return tsat;
    }


    public int getTypeIndex() {
        return THIS_TYPE;
    }


    public int getValueType() {
        return VALUE_TYPE_OBJECT;
    }


    public String getTypeName() {
        return THIS_TYPE_NAME;
    }


    public Set getSet(DataTuple tuple) {
        return (Set) tuple.getObjVal(m_ArrayIndex);
    }


    public void setSet(DataTuple tuple, Set value) {
        tuple.setObjectVal(value, m_ArrayIndex);
    }


    public String getString(DataTuple tuple) {
        Set ts_data = (Set) tuple.getObjVal(getArrayIndex());
        return ts_data.toString();
    }


    public ClusSerializable createRowSerializable() throws ClusException {
        return new MySerializable();
    }


    public boolean isEqualLength() {
        return m_isEqualLength;
    }


    public void writeARFFType(PrintWriter wrt) throws ClusException {
        wrt.print("Set");
    }


    public void setTypeDefinition(String typeDefinition) {
        this.typeDefinition = typeDefinition;
    }


    public String getTypeDefinition() {
        return typeDefinition;
    }


    public void setNumberOfPossibleValues(int numberOfPossibleValues) {
        this.numberOfPossibleValues = numberOfPossibleValues;
    }


    public int getNumberOfPossibleValues() {
        return numberOfPossibleValues;
    }

    public class MySerializable extends ClusSerializable {

        public String getString(DataTuple tuple) {
            Set ts_data = (Set) tuple.getObjVal(0);
            Object[] data = ts_data.getValues();
            String str = "[";
            for (int k = 0; k < data.length; k++) {
                str.concat(String.valueOf(data[k]));
                if (k < (data.length - 1))
                    str.concat(", ");
            }
            str.concat("]");
            return str;
        }


        public boolean read(ClusReader data, DataTuple tuple) throws IOException {
            String str = data.readSet();
            if (str == null)
                return false;
            Set value = new Set(str, getTypeDefinition());
            tuple.setObjectVal(value, getArrayIndex());
            return true;
        }
    }
}