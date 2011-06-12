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

package org.openmole.ide.core.properties

import org.openide.util.Lookup
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.palette.TaskDataProxyFactory

object TaskDisplay extends IDisplay{
  private var count= 0
  private var modelTasks = new HashSet[TaskDataProxyFactory]
  var name="task0"
  var currentPanel: Option[ITaskPanelUI] = None
  
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).foreach(f=>{modelTasks += new TaskDataProxyFactory(f)})
  
  override def implementationClasses = modelTasks
  
  override def dataProxyUI(n: String) = ElementFactories.getTaskDataProxyUI(n)
  
  override def  buildPanelUI(n:String) = {
    currentPanel = Some(dataProxyUI(n).dataUI.buildPanelUI)
    currentPanel.get
  }
  
  override def saveContent(oldName: String) = dataProxyUI(oldName).dataUI = currentPanel.getOrElse(throw new GUIUserBadDataError("No panel to print for entity " + oldName)).saveContent(name)
  
  override def increment = {
    println("INCREMENT ")
    count += 1
    println("INCREMENT " + count)
    name = "task" + count
  }
  
}
