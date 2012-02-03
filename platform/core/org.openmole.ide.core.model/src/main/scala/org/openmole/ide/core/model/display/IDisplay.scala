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

package org.openmole.ide.core.model.display

import scala.collection.mutable.HashSet
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.misc.widget.PopupMenu
import org.openmole.ide.core.model.dataproxy.IDataProxyFactory

trait IDisplay{
  def implementationClasses: HashSet[_<:IDataProxyFactory]
  
  def currentDataProxy: Option[IDataProxyUI]
  
 // def buildPanelUI: IPanelUI
  
  def saveContent
  
 // def setCurrentDataProxy(pID: Int)
  
 // def firstManagementMenu: PopupMenu
}
