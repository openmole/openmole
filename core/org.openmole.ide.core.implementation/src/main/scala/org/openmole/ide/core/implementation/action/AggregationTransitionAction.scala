/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.action

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.workflow.ConnectorWidget
import org.openmole.ide.core.model.workflow.ITransitionUI

class AggregationTransitionAction(connectionWidget: ConnectorWidget) extends ActionListener {
  override def actionPerformed(ae: ActionEvent) = {
    connectionWidget.connector match {
      case x: ITransitionUI ⇒
        if (x.transitionType == BASIC_TRANSITION) x.transitionType = AGGREGATION_TRANSITION
        else x.transitionType = BASIC_TRANSITION
        connectionWidget.drawTransitionType
        CheckData.checkMole(connectionWidget.scene)
      case _ ⇒
    }
  }
}
