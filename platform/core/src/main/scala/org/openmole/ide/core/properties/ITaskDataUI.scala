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

package org.openmole.ide.core.properties

import java.awt.Color
import org.openmole.core.implementation.task.GenericTask
import org.openmole.ide.core.dataproxy._
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.commons.Constants
import scala.collection.mutable.HashSet

trait ITaskDataUI extends IDataUI{
  override def entityType = Constants.TASK
  
  def borderColor: Color
  
  def backgroundColor: Color
  
  def coreObject: GenericTask
  
  def buildTask: GenericTask 
  
  def prototypesIn: HashSet[PrototypeDataProxyUI] 
  
  def prototypesIn_=(pi: HashSet[PrototypeDataProxyUI])
  
  def prototypesOut: HashSet[PrototypeDataProxyUI]
  
  def prototypesOut_=(po: HashSet[PrototypeDataProxyUI])

  def addPrototype(p: PrototypeDataProxyUI, ioType: IOType.Value)
  
  def sampling: Option[SamplingDataProxyUI]
  
  def sampling_=(s: Option[SamplingDataProxyUI])
  
  def environment: Option[EnvironmentDataProxyUI]
  
  def environment_=(s: Option[EnvironmentDataProxyUI])
  
  def buildPanelUI: ITaskPanelUI
}
