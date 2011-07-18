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

package org.openmole.ide.core.implementation.serializer

import org.openmole.core.model.capsule.ICapsule
import org.openmole.core.model.capsule.IExplorationCapsule
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.core.model.mole.IMole
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.capsule._
import org.openmole.core.implementation.transition._
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.implementation.workflow.MoleSceneManager
import org.openmole.ide.core.implementation.workflow.TransitionUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.ITransitionUI
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

object MoleMaker {
  
  var doneCapsules = new HashMap[ICapsuleUI,IGenericCapsule]
  
  def buildMole(manager: IMoleSceneManager):IMole = {
    doneCapsules.clear
    if (manager.startingCapsule.isDefined){
      new Mole(nextCapsule(manager,manager.startingCapsule.get))
    }
    else throw new GUIUserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")  
  }
  
  def nextCapsule(manager: IMoleSceneManager,capsuleUI: ICapsuleUI): IGenericCapsule = {
    val capsule = buildCapsule(capsuleUI)
    doneCapsules+= capsuleUI-> capsule
    manager.capsuleConnections(capsuleUI).foreach(t=>{
        buildTransition(capsule,doneCapsules.getOrElseUpdate(t.target.capsule,nextCapsule(manager,t.target.capsule)),t)
      })
    capsule
  }

  def buildCapsule(capsule: ICapsuleUI) = {
    println("buildCapsule:: " + capsule.capsuleType)
    capsule.capsuleType match {
      case EXPLORATION_TASK=> new ExplorationCapsule(capsule.dataProxy.get.dataUI.buildTask.asInstanceOf[ExplorationTask])
      case BASIC_TASK=> new Capsule(capsule.dataProxy.get.dataUI.buildTask.asInstanceOf[Task])
      case CAPSULE=> new Capsule
    }
  }
  
  def buildTransition(sourceCapsule: IGenericCapsule, targetCapsule: IGenericCapsule,t: ITransitionUI){
    sourceCapsule match {
      case x: IExplorationCapsule=> new ExplorationTransition(x,targetCapsule)
      case z: ICapsule => {
          t.transitionType match {
            case AGGREGATION_TRANSITION=> new AggregationTransition(z,targetCapsule)
            case BASIC_TRANSITION=> new Transition(z,targetCapsule) 
            
          }
        }
      case _=> throw new GUIUserBadDataError("No matching type for capsule " + sourceCapsule +". The transition can not be built")
    }
  }
}