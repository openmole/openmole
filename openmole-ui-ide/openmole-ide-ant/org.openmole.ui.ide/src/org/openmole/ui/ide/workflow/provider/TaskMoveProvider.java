/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.ide.workflow.provider;

import java.awt.Point;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskMoveProvider implements MoveProvider{
        private Point original;

    @Override
    public void movementStarted(Widget widget) {
        System.out.println("Movement start "+widget.toString());
    }

    @Override
    public void movementFinished(Widget widget) {
        System.out.println("Movement fineshed "+widget.toString());
    }

public Point getOriginalLocation (Widget widget) {
            return widget.getPreferredLocation ();
        }

        public void setNewLocation (Widget widget, Point location) {
            widget.setPreferredLocation (location);
        }

}
