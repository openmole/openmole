/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.prototype.base

import org.openmole.core.implementation.data.Prototype
import org.openmole.ide.core.properties.IPrototypeFactoryUI

class BasePrototypeFactoryUI extends IPrototypeFactoryUI {
  var panelData = new BasePrototypePanelUIData
  
  override def panelUIData = panelData
  
  override def coreObject = new Prototype(panelUIData.name,coreClass)
  
  override def coreClass = classOf[Prototype[_]]
  
  override def imagePath = "img/thumb/" + panelData.simpleTypeString.toLowerCase + ".png"
  
  override def buildPanelUI = new BasePrototypePanelUI(panelData)
}
