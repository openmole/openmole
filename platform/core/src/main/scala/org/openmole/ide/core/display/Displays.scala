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
 
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.commons.Constants._
import org.openmole.ide.core.panel.IPanelUI


object Displays {
  var currentType = ""
  val propertyPanel = new PropertyPanel
  
  def name = currentDisplay.name
  
  def setAsTask = currentType = TASK
  def setAsPrototype = currentType = PROTOTYPE
  def setAsSampling = currentType = SAMPLING
  def setAsEnvironment = currentType = ENVIRONMENT
  def setAsName(n: String) = currentDisplay.name = n
  
  def increment = currentDisplay.increment
  
  def implementationClasses = currentDisplay.implementationClasses
  
  def dataProxy = currentDisplay.dataProxyUI(name)
  
  def buildPanelUI = currentDisplay.buildPanelUI(name)
  
  def saveContent(oldName: String, newName: String) = {
    setAsName(newName)
    currentDisplay.saveContent(oldName)
  }
    
  private def currentDisplay :IDisplay= {
    currentType match{
      case TASK=> TaskDisplay
      case PROTOTYPE=> PrototypeDisplay
      case SAMPLING=> SamplingDisplay
      case ENVIRONMENT=> EnvironmentDisplay
      case _=> throw new GUIUserBadDataError("Unknown type " + currentType + " No display is available.")
    }
  }
}
