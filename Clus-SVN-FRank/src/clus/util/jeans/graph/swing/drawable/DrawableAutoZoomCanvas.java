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

package clus.util.jeans.graph.swing.drawable;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


public class DrawableAutoZoomCanvas extends DrawableCanvas {

    public final static long serialVersionUID = 1;


    public DrawableAutoZoomCanvas() {
        addComponentListener(new MyResizeListener());
    }

    private class MyResizeListener extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            setRenderState(STATE_RENDER_AND_SIZE);
        }
    }
}