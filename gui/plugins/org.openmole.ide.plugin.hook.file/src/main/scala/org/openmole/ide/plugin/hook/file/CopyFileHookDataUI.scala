/*
 * Copyright (C) 2011 Mathieu Leclaire
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

import java.io.File
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.tools.Counter
import org.openmole.plugin.hook.file._

class CopyFileHookDataUI(val name: String = "",
                         val toBeHooked: List[(IPrototypeDataProxyUI, String)] = List.empty,
                         val id: Int = Counter.id.getAndIncrement) extends IHookDataUI {

  def coreClass = classOf[CopyFileHook]

  def coreObject(executionManager: IExecutionManager) = toBeHooked.map { h ⇒
    CopyFileHook(
      executionManager.prototypeMapping(h._1).asInstanceOf[Prototype[File]],
      h._2).toHook
  }

  def buildPanelUI = new CopyFileHookPanelUI(this)
}
