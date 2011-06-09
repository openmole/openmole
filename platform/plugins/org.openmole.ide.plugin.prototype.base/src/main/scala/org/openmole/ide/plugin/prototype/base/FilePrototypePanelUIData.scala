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

package org.openmole.ide.plugin.prototype.base
import org.openmole.ide.core.properties.IPrototypeDataUI
import java.io.File
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IPrototype

class FilePrototypeDataUI(var name: String) extends IPrototypeDataUI{
  override def coreClass = classOf[IPrototype[File]]
  
  override def coreObject = new Prototype(name,classOf[File])
  
  override def imagePath = "img/thumb/file.png"
  
  override def buildPanelUI = new FilePrototypePanelUI
}
