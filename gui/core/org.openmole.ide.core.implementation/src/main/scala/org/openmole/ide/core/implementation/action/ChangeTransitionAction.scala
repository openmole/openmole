/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.workflow.ConnectorWidget
import org.openmole.ide.core.model.workflow.ITransitionUI
import scala.swing.Action

class ChangeTransitionAction(connectionWidget: ConnectorWidget,
                             newType: TransitionType) extends Action(TransitionType.toString(newType).toLowerCase) {
  override def apply = {
    connectionWidget.connector match {
      case x: ITransitionUI ⇒
        if (x.transitionType != newType) x.transitionType = newType
        connectionWidget.drawTransitionType
        connectionWidget.scene.manager.invalidateCache
        CheckData.checkMole(connectionWidget.scene)
      case _ ⇒
    }
  }
}
