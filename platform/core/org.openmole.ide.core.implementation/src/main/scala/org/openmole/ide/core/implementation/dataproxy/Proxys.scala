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
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.palette.FrozenProxys
import org.openmole.ide.core.implementation.palette.PaletteSupport
import org.openmole.ide.core.model.data._
import scala.collection.JavaConversions._
import scala.collection.mutable.WeakHashMap
import java.io.File

object Proxys {
    
  val incr = new AtomicInteger
  
  var tasks = new WeakHashMap[Int,ITaskDataProxyUI]
  var prototypes = new WeakHashMap[Int,IPrototypeDataProxyUI]
  var samplings = new WeakHashMap[Int,ISamplingDataProxyUI]
  var environments = new WeakHashMap[Int,IEnvironmentDataProxyUI]
  var domains = new WeakHashMap[Int,IDomainDataProxyUI]
      
  def task = {
    PaletteSupport.currentMoleSceneTopComponent match {
      case None => tasks.toMap
      case _=> if (FrozenProxys.maps.contains(PaletteSupport.currentMoleSceneTopComponent.get)) {
          FrozenProxys.task(PaletteSupport.currentMoleSceneTopComponent.get)}
        else {tasks.toMap}}}
  
  def prototype= PaletteSupport.currentMoleSceneTopComponent match {
    case None => prototypes.toMap
    case _=> if (FrozenProxys.maps.contains(PaletteSupport.currentMoleSceneTopComponent.get)) 
      FrozenProxys.prototype(PaletteSupport.currentMoleSceneTopComponent.get)
      else prototypes.toMap}
  
  def sampling = PaletteSupport.currentMoleSceneTopComponent match {
    case None => samplings.toMap
    case _=> if (FrozenProxys.maps.contains(PaletteSupport.currentMoleSceneTopComponent.get)) 
      FrozenProxys.sampling(PaletteSupport.currentMoleSceneTopComponent.get)
      else samplings.toMap}
  
  def domain = PaletteSupport.currentMoleSceneTopComponent match {
    case None => domains.toMap
    case _=> if (FrozenProxys.maps.contains(PaletteSupport.currentMoleSceneTopComponent.get)) 
      FrozenProxys.domain(PaletteSupport.currentMoleSceneTopComponent.get)
      else domains.toMap}
  
  def environment = PaletteSupport.currentMoleSceneTopComponent match {
    case None => environments.toMap
    case _=> if (FrozenProxys.maps.contains(PaletteSupport.currentMoleSceneTopComponent.get)) 
      FrozenProxys.environment(PaletteSupport.currentMoleSceneTopComponent.get)
      else environments.toMap}
  
  def filePrototypes: List[IPrototypeDataProxyUI] = prototypes.values.filter(_.dataUI.coreObject.`type`.erasure == classOf[File])
  .toList
  
  def isExplorationTaskData(pud: ITaskDataUI) = pud.coreClass.isAssignableFrom(classOf[ExplorationTask]) 
  
  def addTaskElement(dpu: ITaskDataProxyUI) = tasks += incr.getAndIncrement->dpu
  def addPrototypeElement(dpu: IPrototypeDataProxyUI) = prototypes += incr.getAndIncrement->dpu
  def addSamplingElement(dpu: ISamplingDataProxyUI) = samplings += incr.getAndIncrement->dpu
  def addEnvironmentElement(dpu: IEnvironmentDataProxyUI) = environments += incr.getAndIncrement->dpu
  def addDomainElement(dpu: IDomainDataProxyUI) = domains += incr.getAndIncrement->dpu
  
  def addTaskElement(dpu: ITaskDataProxyUI, id:Int) = tasks += id->dpu
  def addPrototypeElement(dpu: IPrototypeDataProxyUI,id:Int) = prototypes += id->dpu
  def addSamplingElement(dpu: ISamplingDataProxyUI,id:Int) = samplings += id->dpu
  def addEnvironmentElement(dpu: IEnvironmentDataProxyUI,id:Int) = environments += id->dpu
  def addDomainElement(dpu: IDomainDataProxyUI,id:Int) = domains += id->dpu
  
  def clearAllTaskElement = tasks.clear
  def clearAllPrototypeElement = prototypes.clear
  def clearAllSamplingElement = samplings.clear
  def clearAllEnvironmentElement = environments.clear
  def clearAllDomainElement = domains.clear
  
  def clearAll: Unit = {
    clearAllTaskElement
    clearAllPrototypeElement
    clearAllSamplingElement
    clearAllEnvironmentElement
    clearAllDomainElement
    FrozenProxys.clear
  }
  
} 
  
