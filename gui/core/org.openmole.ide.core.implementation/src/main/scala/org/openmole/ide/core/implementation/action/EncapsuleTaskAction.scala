/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.action

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene

class EncapsuleTaskAction(moleScene: IMoleScene,
                          capsule: CapsuleUI,
                          dpu: TaskDataProxyUI) extends ActionListener {
  override def actionPerformed(ae: ActionEvent) = {
    capsule.encapsule(dpu)
    moleScene.refresh
  }
}