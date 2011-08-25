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
import org.openmole.plugin.hook.display.GlobalToStringHook
import org.openmole.ide.core.model.data.IHookDataUI
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.model.control.IExecutionManager

class DisplayHookDataUI(executionManager: IExecutionManager,
                        toBeHooked: Map[ICapsule,Iterable[IPrototype[_]]]) extends IHookDataUI{
  def this(exeM: IExecutionManager) = this(exeM,Map.empty[ICapsule,Iterable[IPrototype[_]]])
  
  override def coreObject = new GlobalToStringHook(executionManager.moleExecution,executionManager.printStream,toBeHooked)
  
  override def coreClass = classOf[Object]
  
  override def buildPanelUI = new DisplayHookPanelUI(executionManager)
}
