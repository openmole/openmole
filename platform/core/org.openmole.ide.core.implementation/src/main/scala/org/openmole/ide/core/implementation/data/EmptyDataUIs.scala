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

package org.openmole.ide.core.implementation.data

import org.openmole.ide.core.model.data.IPrototypeDataUI
import org.openmole.ide.core.model.panel.IPrototypePanelUI
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IPrototype

object EmptyDataUIs {
  
  class  EmptyPrototypeDataUI extends IPrototypeDataUI[Any]  {
    def name = ""
    def dim = 0
    def coreClass = classOf[IPrototype[_]]
    def coreObject = new Prototype("empty",classOf[Any])
    def imagePath = ""
    def buildPanelUI = new EmptyPrototypePanelUI
    def displayTypedName = ""
    
    class EmptyPrototypePanelUI extends IPrototypePanelUI[Any] {
      override def peer = this.peer
      def saveContent(name: String) = new EmptyPrototypeDataUI}
  }
}
