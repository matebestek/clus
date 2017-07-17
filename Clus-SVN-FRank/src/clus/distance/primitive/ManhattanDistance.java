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

package clus.distance.primitive;

import clus.data.rows.DataTuple;
import clus.data.type.ClusAttrType;
import clus.distance.ClusDistance;
import clus.main.settings.Settings;


/**
 * @author Mitja Pugelj
 */

public class ManhattanDistance extends ClusDistance {

    private static final long serialVersionUID = Settings.SERIAL_VERSION_ID;
    private SearchDistance m_Search;


    public ManhattanDistance(SearchDistance search) {
        m_Search = search;
    }


    public double calcDistance(DataTuple t1, DataTuple t2) {
        double dist = 0;
        for (ClusAttrType attr : t1.getSchema().getAllAttrUse(ClusAttrType.ATTR_USE_DESCRIPTIVE))
            dist += m_Search.calcDistanceOnAttr(t1, t2, attr);
        return dist;
    }


    @Override
    public String getDistanceName() {
        return "Manhattan distance";
    }

}