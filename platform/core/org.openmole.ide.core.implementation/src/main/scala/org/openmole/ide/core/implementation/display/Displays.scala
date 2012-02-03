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
 
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.display.IDisplay
import org.openmole.ide.core.model.commons.Constants._


object Displays {
  var currentType = TASK
  val propertyPanel = new PropertyPanel
  var currentProxyID = 0
  var initMode = false
  
  def name = if(dataProxy.isDefined) dataProxy.get.dataUI.name else ""
  
 // def setCurrentProxyID(pID: Int) = {
  //  currentDisplay.setCurrentDataProxy(pID)
   // currentProxyID = pID}
    
  def implementationClasses = currentDisplay.implementationClasses
  
  def dataProxy = currentDisplay.currentDataProxy
  
//  def buildPanelUI = currentDisplay.buildPanelUI
  
  def saveContent = currentDisplay.saveContent
  
 // def firstManagementMenu = currentDisplay.firstManagementMenu
    
  private def currentDisplay :IDisplay = 
    currentType match{
//      case TASK=> TaskDisplay
//      case PROTOTYPE=> PrototypeDisplay
//      case SAMPLING=> SamplingDisplay
//      case ENVIRONMENT=> EnvironmentDisplay
      case _=> throw new UserBadDataError("Unknown type " + currentType + " No display is available.")
  }
}
