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

package org.openmole.ide.core.implementation.dataproxy

import org.openide.util.Lookup
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.core.implementation.data.TaskDataUI
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WeakHashMap

object Proxys {
    
  var task = new HashSet[TaskDataProxyUI]
  var prototype = new HashSet[PrototypeDataProxyUI]
  var sampling = new HashSet[SamplingDataProxyUI]
  var environment = new HashSet[EnvironmentDataProxyUI]
  
  def isExplorationTaskData(pud: ITaskDataUI) = pud.coreClass.isAssignableFrom(classOf[ExplorationTask]) 

  
  def getTaskDataProxyUI(name: String) = task.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getPrototypeDataProxyUI(name: String) = prototype.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getSamplingDataProxyUI(name: String) = sampling.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getEnvironmentDataProxyUI(name: String) = environment.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  
  
  def getPrototypesNames = task.groupBy(_.dataUI.name).keys.toSet
  
  def addTaskElement(dpu: TaskDataProxyUI) = task += dpu
  def addPrototypeElement(dpu: PrototypeDataProxyUI) = prototype += dpu
  def addSamplingElement(dpu: SamplingDataProxyUI) = sampling += dpu
  def addEnvironmentElement(dpu: EnvironmentDataProxyUI) = environment += dpu
  
  
  def removeTaskElement(dpu: TaskDataProxyUI) = task.remove(dpu)
  def removePrototypeElement(dpu: PrototypeDataProxyUI) = prototype.remove(dpu)
  def removeSamplingElement(dpu: SamplingDataProxyUI) = sampling.remove(dpu)
  def removeEnvironmentElement(dpu: EnvironmentDataProxyUI) = environment.remove(dpu)
  
  def clearAllTaskElement = task.clear
  def clearAllPrototypeElement = prototype.clear
  def clearAllSamplingElement = sampling.clear
  def clearAllEnvironmentElement = environment.clear
  
  def clearAll: Unit = {
    clearAllTaskElement
    clearAllPrototypeElement
    clearAllSamplingElement
    clearAllEnvironmentElement
  }
  
} 
  
