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

package org.openmole.ide.plugin.hook.file

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.tools.Counter
import org.openmole.plugin.hook.file.AppendToCSVFileHook
import org.openmole.ide.core.implementation.registry._

class AppendToCSVFileHookDataUI(var activated: Boolean = true,
                                val prototypes: Iterable[IPrototypeDataProxyUI] = List.empty,
                                val fileName: String = "",
                                val id: Int = Counter.id.getAndIncrement) extends IHookDataUI {

  def buildPanelUI(task: ITaskDataProxyUI) = new AppendToCSVFileHookPanelUI(this, task)

  def coreClass = classOf[AppendToCSVFileHook]

  def coreObject(executionManager: IExecutionManager) = {
    List(new AppendToCSVFileHook(
      fileName,
      prototypes.map { executionManager.prototypeMapping }.toSeq: _*))
  }
}