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

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.data._
import scala.collection.JavaConversions._
import scala.collection.mutable.WeakHashMap

object Proxys {
    
  val incr = new AtomicInteger
  
  var task = new WeakHashMap[Int,ITaskDataProxyUI]
  var prototype = new WeakHashMap[Int,IPrototypeDataProxyUI]
  var sampling = new WeakHashMap[Int,ISamplingDataProxyUI]
  var environment = new WeakHashMap[Int,IEnvironmentDataProxyUI]
  var domain = new WeakHashMap[Int,IDomainDataProxyUI]
  
  def isExplorationTaskData(pud: ITaskDataUI) = pud.coreClass.isAssignableFrom(classOf[ExplorationTask]) 
  
  def addTaskElement(dpu: ITaskDataProxyUI) = task += incr.getAndIncrement->dpu
  def addPrototypeElement(dpu: IPrototypeDataProxyUI) = prototype += incr.getAndIncrement->dpu
  def addSamplingElement(dpu: ISamplingDataProxyUI) = sampling += incr.getAndIncrement->dpu
  def addEnvironmentElement(dpu: IEnvironmentDataProxyUI) = environment += incr.getAndIncrement->dpu
  def addDomainElement(dpu: IDomainDataProxyUI) = domain += incr.getAndIncrement->dpu
  
  def addTaskElement(dpu: ITaskDataProxyUI, id:Int) = task += id->dpu
  def addPrototypeElement(dpu: IPrototypeDataProxyUI,id:Int) = prototype += id->dpu
  def addSamplingElement(dpu: ISamplingDataProxyUI,id:Int) = sampling += id->dpu
  def addEnvironmentElement(dpu: IEnvironmentDataProxyUI,id:Int) = environment += id->dpu
  def addDomainElement(dpu: IDomainDataProxyUI,id:Int) = domain += id->dpu
  
  def clearAllTaskElement = task.clear
  def clearAllPrototypeElement = prototype.clear
  def clearAllSamplingElement = sampling.clear
  def clearAllEnvironmentElement = environment.clear
  def clearAllDomainElement = domain.clear
  
  def clearAll: Unit = {
    clearAllTaskElement
    clearAllPrototypeElement
    clearAllSamplingElement
    clearAllEnvironmentElement
    clearAllDomainElement
  }
  
} 
  
