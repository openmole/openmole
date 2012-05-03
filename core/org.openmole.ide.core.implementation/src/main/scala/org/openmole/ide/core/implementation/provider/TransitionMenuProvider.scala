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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.implementation.action.AddTransitionConditionAction
import org.openmole.ide.core.implementation.action.AggregationTransitionAction
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.workflow.ConnectorWidget

class TransitionMenuProvider(scene: MoleScene, connectionWidget: ConnectorWidget) extends GenericMenuProvider {
  val itCond = new JMenuItem("Edit condition")
  itCond.addActionListener(new AddTransitionConditionAction(connectionWidget))

  var itAgreg = new JMenuItem
  items += (itCond, itAgreg)

  override def getPopupMenu(widget: Widget, point: Point) = {
    items -= itAgreg
    if (!(connectionWidget.transition.transitionType == EXPLORATION_TRANSITION)) {
      var transitonTypeString = if (connectionWidget.transition.transitionType == BASIC_TRANSITION) "aggregation" else "basic"
      itAgreg = new JMenuItem("Set as " + transitonTypeString + " transition")
      itAgreg.addActionListener(new AggregationTransitionAction(connectionWidget))
      items += itAgreg
    }
    super.getPopupMenu(widget, point)
  }
}