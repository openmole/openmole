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
import org.openmole.ide.core.implementation.dataproxy.EnvironmentDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.misc.widget.PopupMenu
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.core.model.factory.IEnvironmentFactoryUI
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.display.IEnvironmentDisplay
import org.openmole.ide.core.implementation.action.DetachEnvironmentAction
import scala.swing.MenuItem

object EnvironmentDisplay extends IEnvironmentDisplay{
  private var modelEnvironments = new HashSet[EnvironmentDataProxyFactory]
  var currentPanel: Option[IEnvironmentPanelUI] = None
  var currentDataProxy: Option[IEnvironmentDataProxyUI] = None
  
  Lookup.getDefault.lookupAll(classOf[IEnvironmentFactoryUI]).foreach(f=>{modelEnvironments += new EnvironmentDataProxyFactory(f)})
  
 // override def setCurrentDataProxy(pID: Int) = currentDataProxy = Some(Proxys.environment(pID))
  
  override def implementationClasses = modelEnvironments
  
//  override def  buildPanelUI = {
//   // currentPanel = Some(currentDataProxy.get.dataUI.buildPanelUI)
//   // currentPanel.get
//  }
//  
//  override def firstManagementMenu= new PopupMenu{
//    add(new MenuItem(new RemoveEnvironmentAction(Displays.currentProxyID)))}
//  
//  override def secondManagementMenu(taskProxy: ITaskDataProxyUI,environmentProxy: IEnvironmentDataProxyUI) = {
//    new PopupMenu {
//    add(new MenuItem(new DetachEnvironmentAction(Some(taskProxy))))}}
//  
  
  override def saveContent = {
    //currentDataProxy.get.dataUI = currentPanel.getOrElse(throw new UserBadDataError("No panel to print for entity " + name)).saveContent
    if (Displays.initMode) Proxys.environments += currentDataProxy.get
  }
   
}