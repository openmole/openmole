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

package org.openmole.ide.plugin.hook.display

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.plugin.hook.display.ToStringHook
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.control.IExecutionManager

class ToStringHookDataUI(var activated: Boolean = true,
                         val toBeHooked: List[IPrototypeDataProxyUI] = List.empty) extends IHookDataUI {

  def coreClass = classOf[ToStringHook]

  def coreObject(executionManager: IExecutionManager,
                 moleExecution: IMoleExecution,
                 capsule: ICapsule) =
    List(new ToStringHook(moleExecution,
      capsule,
      executionManager.printStream,
      toBeHooked.map { executionManager.prototypeMapping }.toSeq: _*))

  def buildPanelUI(task: ITaskDataProxyUI) = new ToStringHookPanelUI(this, task)
}
