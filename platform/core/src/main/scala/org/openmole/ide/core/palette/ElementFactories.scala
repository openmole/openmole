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

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IDataUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.properties._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WeakHashMap

object ElementFactories {
  
//  lazy val dataProxys = Map(Constants.TASK -> new ListBuffer[DataProxyUI[TaskDataUI]],
//                                 Constants.PROTOTYPE -> new ListBuffer[DataProxyUI[DataUI[Prototype[_]]]],
//                                 Constants.SAMPLING -> new ListBuffer[DataProxyUI[DataUI[ISampling]]],
//                                 Constants.ENVIRONMENT -> new ListBuffer[DataProxyUI[DataUI[IEnvironment]]])
  
  var dataTaskProxys = new HashSet[TaskDataProxyUI]
  var dataPrototypeProxys = new HashSet[PrototypeDataProxyUI]
  var dataSamplingProxys = new HashSet[SamplingDataProxyUI]
  var dataEnvironmentProxys = new HashSet[EnvironmentDataProxyUI]
  
  
//  lazy val dataProxys = Map(Constants.TASK -> new ListBuffer[DataProxyUI],
//                            Constants.PROTOTYPE -> new ListBuffer[DataProxyUI],
//                            Constants.SAMPLING -> new ListBuffer[DataProxyUI],
//                            Constants.ENVIRONMENT -> new ListBuffer[DataProxyUI])
  
  
//  def updateLookup(factoryClass: Class[_<:IFactoryUI], entityType: String) = {
//    val li = new ListBuffer[ModelElementFactory]
//    Lookup.getDefault.lookupAll(factoryClass).foreach(p=>{li += new ModelElementFactory(p)})
//    li
//  }

 // def updateData(pud: IDataUI,oldName: String) = getDataProxyUI(pud.entityType,oldName).dataUI = pud
  
  def isExplorationTaskData(pud: ITaskDataUI) = pud.coreClass.isAssignableFrom(classOf[ExplorationTask]) 

  // def getDataProxyUI(categoryName: String, name: String) = dataProxys(categoryName).groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
//  def getDataProxyUI(categoryName: String, name: String) = {
//    categoryName match {
//      case Constants.TASK=> dataTaskProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
//      case Constants.PROTOTYPE=> dataPrototypeProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
//      case Constants.SAMPLING=> dataSamplingProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
//      case Constants.ENVIRONMENT=> dataEnvironmentProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
//    }
//  }
  
  def getTaskDataProxyUI(name: String) = dataTaskProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getPrototypeDataProxyUI(name: String) = dataPrototypeProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getSamplingDataProxyUI(name: String) = dataSamplingProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  def getEnvironmentDataProxyUI(name: String) = dataEnvironmentProxys.groupBy(_.dataUI.name).filterKeys(k => k.equals(name)).getOrElse(name,throw new GUIUserBadDataError("Not found entity " + name)).head
  
  
  
  // def getAll(entityType: String) = dataProxys(entityType)
  
  def addTaskElement(dpu: TaskDataProxyUI) = dataTaskProxys += dpu
  def addPrototypeElement(dpu: PrototypeDataProxyUI) = dataPrototypeProxys += dpu
  def addSamplingElement(dpu: SamplingDataProxyUI) = dataSamplingProxys += dpu
  def addEnvironmentElement(dpu: EnvironmentDataProxyUI) = dataEnvironmentProxys += dpu
  
  
  def removeTaskElement(dpu: TaskDataProxyUI) = dataTaskProxys.remove(dpu)
  def removePrototypeElement(dpu: PrototypeDataProxyUI) = dataPrototypeProxys.remove(dpu)
  def removeSamplingElement(dpu: SamplingDataProxyUI) = dataSamplingProxys.remove(dpu)
  def removeEnvironmentElement(dpu: EnvironmentDataProxyUI) = dataEnvironmentProxys.remove(dpu)
  
  def clearAllTaskElement = dataTaskProxys.clear
  def clearAllPrototypeElement = dataPrototypeProxys.clear
  def clearAllSamplingElement = dataSamplingProxys.clear
  def clearAllEnvironmentElement = dataEnvironmentProxys.clear
  
  def clearAll: Unit = {
    clearAllTaskElement
    clearAllPrototypeElement
    clearAllSamplingElement
    clearAllEnvironmentElement
  }
  
} 
  