/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.ui.ide.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class AddExistingPrototypeAction implements ActionListener {

    private PrototypeUI prototype;
    private CapsuleViewUI capsuleViewUI;
    private IOType type;

    public AddExistingPrototypeAction(PrototypeUI prototype,
                                CapsuleViewUI capsuleViewUI,
                                IOType type) {
        this.prototype = prototype;
        this.capsuleViewUI = capsuleViewUI;
        this.type = type;
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        capsuleViewUI.getCapsuleModel().getTaskModel().addPrototype(prototype, type);
        capsuleViewUI.repaint();
    }

}
