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
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.TransitionType._
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.action.AddTransitionConditionAction
import org.openmole.ide.core.implementation.action.AggregationTransitionAction
import org.openmole.ide.core.implementation.action.RemoveTransitionAction
import org.openmole.ide.core.implementation.workflow.TransitionUI
import org.openmole.ide.core.implementation.workflow.DataChannelUI
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.workflow.ConnectorWidget
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.model.workflow.ITransitionUI
import scala.swing.Action
import scala.swing.MenuItem

class ConnectorMenuProvider(scene: MoleScene,
                            connectionWidget: ConnectorWidget) extends GenericMenuProvider {

  var itAgreg = new JMenuItem
  var itChange = new MenuItem("to")

  connectionWidget.connector match {
    case x: ITransitionUI ⇒
      val itCond = new JMenuItem("Edit condition")
      itCond.addActionListener(new AddTransitionConditionAction(connectionWidget))

      items += (itCond, itAgreg, itChange.peer)
    case _ ⇒
  }

  val itRem = new JMenuItem("Remove")
  itRem.addActionListener(new RemoveTransitionAction(scene, scene.manager.connectorMap.getKey(connectionWidget.connector)))

  items += itRem

  override def getPopupMenu(widget: Widget, point: Point) = {
    items -= itChange.peer
    connectionWidget.connector match {
      case x: ITransitionUI ⇒
        items -= (itAgreg, itChange.peer)
        if (!(x.transitionType == EXPLORATION_TRANSITION)) {
          var transitonTypeString = if (x.transitionType == BASIC_TRANSITION) "aggregation" else "basic"
          itAgreg = new JMenuItem("Set as " + transitonTypeString + " transition")
          itAgreg.addActionListener(new AggregationTransitionAction(connectionWidget))
          items += itAgreg
        }
        itChange = new MenuItem(new Action("to Data channel") {
          override def apply {
            //  scene.manager.removeConnector(scene.manager.connectorID(x))
            val newC = new DataChannelUI(x.source,
              x.target,
              x.filteredPrototypes)
            //  scene.manager.registerConnector(newC)
            scene.manager.changeConnector(x, newC)
            connectionWidget.setConnnector(newC)
          }
        })
      case x: IDataChannelUI ⇒
        itChange = new MenuItem(new Action("to Transition") {
          override def apply {
            //  scene.manager.removeConnector(scene.manager.connectorID(x))
            val newC = new TransitionUI(x.source,
              x.target,
              BASIC_TRANSITION,
              None,
              x.filteredPrototypes)
            scene.manager.changeConnector(x, newC)
            connectionWidget.setConnnector(newC)
            // scene.manager.registerConnector(newC)
          }
        })
      case _ ⇒
    }
    items += itChange.peer
    super.getPopupMenu(widget, point)
  }
}