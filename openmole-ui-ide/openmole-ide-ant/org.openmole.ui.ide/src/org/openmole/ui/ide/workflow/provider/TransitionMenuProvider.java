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
package org.openmole.ui.ide.workflow.provider;

import javax.swing.JMenuItem;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.openmole.ui.ide.workflow.action.AddTransitionConditionAction;
import org.openmole.ui.ide.workflow.action.RemoveTransitionAction;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.paint.LabeledConnectionWidget;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class TransitionMenuProvider extends GenericMenuProvider {

    public TransitionMenuProvider(MoleScene scene,
            LabeledConnectionWidget connectionWidget,
            String edgeID) {
        super();

        JMenuItem itemCondition = new JMenuItem("Edit condition");
        itemCondition.addActionListener(new AddTransitionConditionAction(scene.getManager().getTransition(edgeID),
                connectionWidget));

        JMenuItem itemRemove = new JMenuItem("Remove transition");
        itemRemove.addActionListener(new RemoveTransitionAction(scene, edgeID));

        items.add(itemCondition);
        items.add(itemRemove);
    }
}
