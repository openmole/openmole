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
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.collection.JavaConversions._

object CheckData {
  def checkMole(manager : IMoleSceneManager) = {
    manager.startingCapsule match {
      case Some(x:ICapsuleUI) => 
        val (mole,cMap,pMap) = MoleMaker.buildMole(manager)
        val capsuleMap : Map[ICapsule,ICapsuleUI] = cMap.map{case (k,v) => v -> k}
        val prototypeMap : Map[IPrototype[_],IPrototypeDataProxyUI] = pMap.map{case (k,v) => v -> k}.toMap
        val errors = Validation.typeErrors(mole) ++ Validation.duplicatedTransitions(mole)
        errors.isEmpty match {
          case false => 
            errors.map{
              _ match {
                case x : DataflowProblem => 
                  capsuleMap(x.capsule)-> (prototypeMap(x.data.prototype),x)
              }
            }.groupBy(_._1).map{ case (k,v) => (k,v.map(_._2))}.foreach{
              case(capsuleUI,e)=>
                capsuleUI.updateErrors(e.toList)
            }
          case true => manager.capsules.values.foreach{_.updateErrors(List.empty)}
        }
      case _ =>
    }
  }
}
//   case x : WrongType => println("wrong type on capsule " + capsuleMap(x.capsule))
//   case x : MissingInput => println("missing input "+x.data.prototype.name +" on capsule " + capsuleMap(x.capsule).)