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

package org.openmole.ide.core.display

import org.openide.util.Lookup
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.dataproxy.Proxys
import org.openmole.ide.core.dataproxy.TaskDataProxyFactory
import org.openmole.ide.core.panel.ITaskPanelUI
import org.openmole.ide.core.factory.ITaskFactoryUI

object TaskDisplay extends IDisplay{
  private var modelTasks = new HashSet[TaskDataProxyFactory]
  var currentPanel: Option[ITaskPanelUI] = None
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).foreach(f=>{modelTasks += new TaskDataProxyFactory(f)})
  private var count= modelTasks.size
  var name="task" + count
  
  override def implementationClasses = modelTasks
  
  override def dataProxyUI(n: String) = Proxys.getTaskDataProxyUI(n)
  
  override def  buildPanelUI(n:String) = {
    currentPanel = Some(dataProxyUI(n).dataUI.buildPanelUI)
    currentPanel.get
  }
  
  override def saveContent(oldName: String) = {
    val env = dataProxyUI(oldName).dataUI.environment
    val sample = dataProxyUI(oldName).dataUI.sampling
    val protoI = dataProxyUI(oldName).dataUI.prototypesIn
    val protoO = dataProxyUI(oldName).dataUI.prototypesOut
    dataProxyUI(oldName).dataUI = currentPanel.getOrElse(throw new GUIUserBadDataError("No panel to print for entity " + oldName)).saveContent(name)
    dataProxyUI(name).dataUI.prototypesIn_=(protoI)
    dataProxyUI(name).dataUI.prototypesOut_=(protoO)
    dataProxyUI(name).dataUI.sampling_=(sample)
    dataProxyUI(name).dataUI.environment_=(env)
  }
  
  override def increment = {
    count += 1
    name = "task" + count
  }
  
}
