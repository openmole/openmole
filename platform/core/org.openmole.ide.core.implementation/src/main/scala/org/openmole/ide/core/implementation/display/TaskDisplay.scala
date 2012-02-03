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

package org.openmole.ide.core.implementation.display

import org.openide.util.Lookup
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import org.openmole.ide.misc.widget.PopupMenu
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.display.ITaskDisplay
import org.openmole.ide.core.model.factory.ITaskFactoryUI
import scala.swing.MenuItem

object TaskDisplay extends ITaskDisplay{
  private var modelTasks = new HashSet[TaskDataProxyFactory]
  var currentPanel: Option[ITaskPanelUI] = None
  var currentDataProxy: Option[ITaskDataProxyUI] = None
  
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).foreach(f=>{modelTasks += new TaskDataProxyFactory(f)})
  
 // override def setCurrentDataProxy(pID: Int) = currentDataProxy = Some(Proxys.task(pID))
  
  override def implementationClasses = modelTasks

//  override def buildPanelUI = {
//    currentPanel = Some(currentDataProxy.get.dataUI.buildPanelUI)
//    currentPanel.get
//  }
  
//  override def firstManagementMenu= new PopupMenu{
//   add(new MenuItem(new RemoveTaskAction(Displays.currentProxyID)))}
//  
//  override def secondManagementMenu(damainProxy: ITaskDataProxyUI) = new PopupMenu {}
//  
  override def saveContent = {
    val env = currentDataProxy.get.dataUI.environment
    val sample = currentDataProxy.get.dataUI.sampling
    val protoI = currentDataProxy.get.dataUI.prototypesIn
    val protoO = currentDataProxy.get.dataUI.prototypesOut
   // currentDataProxy.get.dataUI = currentPanel.getOrElse(throw new UserBadDataError("No panel to print for entity " + name)).saveContent(name)
    currentDataProxy.get.dataUI.prototypesIn_=(protoI)
    currentDataProxy.get.dataUI.prototypesOut_=(protoO)
    currentDataProxy.get.dataUI.sampling_=(sample)
    currentDataProxy.get.dataUI.environment_=(env)
    if (Displays.initMode) Proxys.tasks += currentDataProxy.get
  }
}