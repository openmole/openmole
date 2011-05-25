/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.provider

import javax.swing.JMenuItem
import org.openmole.ide.core.workflow.action.AddTransitionConditionAction
import org.openmole.ide.core.workflow.action.AggregationTransitionAction
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget

class TransitionMenuProvider(scene: MoleScene,connectionWidget: LabeledConnectionWidget,edgeID: String) extends GenericMenuProvider {

  val itemCondition = new JMenuItem("Edit condition")
  val itemAggregation = new JMenuItem("Set as aggregation transition")
  itemCondition.addActionListener(new AddTransitionConditionAction(scene.manager.getTransition(edgeID),connectionWidget))
  itemAggregation.addActionListener(new AggregationTransitionAction)
}