/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.data
import org.openmole.core.implementation.validation.DataflowProblem
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry._
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyFactory
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.misc.tools.service.Logger
import scala.collection.JavaConversions._

object CheckData extends Logger {
  def dataProxyFactory(data : IData[_]) =  
    new PrototypeDataProxyFactory(KeyRegistry.prototypes(KeyGenerator(data.prototype))).buildDataProxyUI(data.prototype)
    
  def checkMole(manager : IMoleSceneManager) = 
    manager.startingCapsule match {
      case Some(x : ICapsuleUI) => 
        val (mole,cMap,pMap,errs) = MoleMaker.buildMole(manager)
        val error_capsules = manager.capsules.values.partition{_.dataUI.task.isDefined}
        error_capsules._1.foreach(_.setAsValid)
        error_capsules._2.foreach{_.setAsInvalid("A capsule has to be encapsulated to be run")}
        
          val capsuleMap : Map[ICapsule,ICapsuleUI] = cMap.map{case (k,v) => v -> k}
          val prototypeMap : Map[IPrototype[_],IPrototypeDataProxyUI] = pMap.map{case (k,v) => v -> k}.toMap
        
          // Compute implicit input / output
          capsuleMap.foreach{case(caps,capsUI) => 
              capsUI.dataUI.task match {
                case Some(x : ITaskDataProxyUI) => 
                  x.dataUI.implicitPrototypesIn = caps.inputs.filterNot{c=> prototypeMap.containsKey(c.prototype)}.toList.map{dataProxyFactory}
                  x.dataUI.implicitPrototypesOut = caps.outputs.filterNot{c=> prototypeMap.containsKey(c.prototype)}.toList.map{dataProxyFactory}
                case _ =>
              }
          }
        
          // Formal validation
          val errors = Validation(mole)
          errors.isEmpty match {
            case false => 
              errors.flatMap{
                _ match {
                  case x : DataflowProblem => 
                    Some(capsuleMap(x.capsule)-> (prototypeMap(x.data.prototype),x))
                  case x => 
                    logger.info("Error " + x + " not taken into account in the GUI yet.")
                    None
                }
              }.groupBy(_._1).map{ case (k,v) => (k,v.map(_._2))}.foreach{
                case(capsuleUI,e)=>
                  capsuleUI.updateErrors(e.toList)
              }
            case true => manager.capsules.values.foreach{_.updateErrors(List.empty)}
          }
          Some(mole,capsuleMap,prototypeMap,errs)
          errs.foreach{ case(cui,e) => cui.setAsInvalid(e.getMessage) }
          Some(mole,cMap,pMap,errs)
      case _ => None
    }
  
  def fullCheck(manager : IMoleSceneManager) = {
    val a = checkMole(manager)
    if (a.isDefined)
      if (a.get._4.isEmpty)
        checkTopology(a.get._1)
  }
  
  
  def checkTopology(mole : IMole) = {
    val st = Validation.topologyErrors(mole).mkString("\n")
    if (! st.isEmpty) StatusBar.block(st)
  }
}