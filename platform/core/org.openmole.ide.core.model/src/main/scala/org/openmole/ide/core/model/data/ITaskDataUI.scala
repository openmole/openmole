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

package org.openmole.ide.core.model.data

import java.awt.Color
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.implementation.task.Task
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.ITaskPanelUI

trait ITaskDataUI extends IDataUI{
  
  def borderColor: Color
  
  def backgroundColor: Color
  
  def coreObject: Task
  
  def prototypesIn: List[(IPrototypeDataProxyUI,String)]
  
  def prototypesIn_=(pi: List[(IPrototypeDataProxyUI,String)])
  
  def prototypesOut: List[IPrototypeDataProxyUI]
  
  def prototypesOut_=(po: List[IPrototypeDataProxyUI])
  
  def buildPanelUI: ITaskPanelUI
}
