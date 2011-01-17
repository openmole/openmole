/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.workflow.implementation;

import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget;
import org.openmole.ui.ide.workflow.implementation.paint.OSlotWidget;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class TransitionUI {
//    public ICapsuleModelUI source;
//    public ICapsuleModelUI target;
//    public int targetSlotNumber;

    public CapsuleViewUI source;
    public ISlotWidget target;
//    public int targetSlotNumber;

//    public TransitionUI(ICapsuleModelUI source, ICapsuleModelUI target, int targetSlotNumber) {
//        this.source = source;
//        this.target = target;
//        this.targetSlotNumber = targetSlotNumber;
//    }
    public TransitionUI(CapsuleViewUI source, ISlotWidget target) {
        this.source = source;
        this.target = target;
    }

//    public ICapsuleModelUI getSource() {
//        return source;
//    }
//
//    public ICapsuleModelUI getTarget() {
//        return target;
//    }
//
//    public int getTargetSlotNumber() {
//        return targetSlotNumber;
//    }
//public ICapsuleModelUI getSource() {
//        return source;
//    }
//
//    public ICapsuleModelUI getTarget() {
//        return target;
//    }
    public CapsuleViewUI getSource() {
        return source;
    }

    public ISlotWidget getTarget() {
        return target;
    }
}
