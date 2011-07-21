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
 
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.display.IDisplay
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.IPanelUI


object Displays {
  var currentType = TASK
  val propertyPanel = new PropertyPanel
  
  def name = currentDisplay.name
  
  def setAsName(n: String) = currentDisplay.name = n
  
  def increment = currentDisplay.increment
  
  def implementationClasses = currentDisplay.implementationClasses
  
  def dataProxy = currentDisplay.dataProxy
  
  def buildPanelUI = currentDisplay.buildPanelUI(name)
  
  def saveContent(name: String) = currentDisplay.saveContent(name)
  
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
