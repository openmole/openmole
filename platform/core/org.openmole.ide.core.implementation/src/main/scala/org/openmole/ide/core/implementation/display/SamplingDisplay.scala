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
import org.openmole.ide.core.implementation.action.DetachSamplingAction
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dataproxy.SamplingDataProxyFactory
import org.openmole.ide.misc.widget.PopupMenu
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.display.ISamplingDisplay
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory.ISamplingFactoryUI
import scala.swing.MenuItem

object SamplingDisplay extends ISamplingDisplay{
  private var modelSamplings = new HashSet[SamplingDataProxyFactory]
  var currentPanel: Option[ISamplingPanelUI] = None
  var currentDataProxy: Option[ISamplingDataProxyUI] = None
  
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).foreach(f=>modelSamplings += new SamplingDataProxyFactory(f))
  
//  override def firstManagementMenu= new PopupMenu{
//    add(new MenuItem(new RemoveSamplingAction(Displays.currentProxyID)))}
//  
//  override def secondManagementMenu(taskProxy : ITaskDataProxyUI, samplingProxy: ISamplingDataProxyUI) = new PopupMenu {
//    add(new MenuItem(new DetachSamplingAction(Some(taskProxy))))}
//  
 // override def setCurrentDataProxy(pID: Int) = currentDataProxy = Some(Proxys.samplings(pID))
  
  override def implementationClasses = modelSamplings
  
//  override def  buildPanelUI = {
//    currentPanel = Some(currentDataProxy.get.dataUI.buildPanelUI)
//    currentPanel.get
//  }
  
  override def saveContent = {
   // currentDataProxy.get.dataUI = currentPanel.getOrElse(throw new UserBadDataError("No panel to print for entity " + name)).saveContent(name)
    if (Displays.initMode) Proxys.samplings += currentDataProxy.get}
  
}