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

package clus.error.common;

import clus.data.type.primitive.NominalAttrType;
import clus.main.settings.Settings;
import clus.statistic.ClassificationStat;
import clus.statistic.ClusStatistic;


public abstract class ClusNominalError extends ClusError {

    private static final long serialVersionUID = Settings.SERIAL_VERSION_ID;
    protected int[] m_Default;
    protected NominalAttrType[] m_Attrs;


    public ClusNominalError(ClusErrorList par, NominalAttrType[] nom) {
        super(par, nom.length);
        m_Attrs = nom;
    }


    public NominalAttrType getAttr(int i) {
        return m_Attrs[i];
    }


    public ClusNominalError(ClusErrorList par, int nb_nominal) {
        super(par, nb_nominal);
    }


    public void setDefault(int[] value) {
        m_Default = value;
    }


    public void setDefault(ClusStatistic pred) {
        m_Default = ((ClassificationStat) pred).m_MajorityClasses;
    }
}
