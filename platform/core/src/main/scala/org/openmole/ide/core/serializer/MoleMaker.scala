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

package org.openmole.ide.core.serializer

import org.openmole.core.model.capsule.ICapsule
import org.openmole.core.model.capsule.IExplorationCapsule
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.ide.core.control.MoleScenesManager.TransitionType._
import org.openmole.ide.core.control.MoleScenesManager.CapsuleType._
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.capsule._
import org.openmole.core.implementation.transition._
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.properties.IPanelUIData
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.MoleSceneManager
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import org.openmole.ide.core.workflow.implementation.TransitionUI
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

object MoleMaker {
  
  var doneCapsules = new HashMap[CapsuleViewUI,IGenericCapsule]
  
  def buildMole(manager: MoleSceneManager){
    doneCapsules.clear
    if (manager.startingCapsule.isDefined){
      val mole = new Mole(nextCapsule(manager,manager.startingCapsule.get))
    }
    else throw new GUIUserBadDataError("No starting capsule is defined. The mole construction is not possible. Please define a capsule as a starting capsule.")  
  }
  
  def nextCapsule(manager: MoleSceneManager,capsuleUI: CapsuleViewUI): IGenericCapsule = {
    val capsule = buildCapsule(capsuleUI.capsuleModel)
    doneCapsules+= capsuleUI-> capsule
    manager.capsuleConnections(capsuleUI).foreach(t=>{
        buildTransition(capsule,doneCapsules.getOrElseUpdate(t.target.capsuleView,nextCapsule(manager,t.target.capsuleView)),t)
      })
    capsule
  }

  def buildCapsule(capsuleModel: ICapsuleModelUI) = {
    capsuleModel.capsuleType match {
      case EXPLORATION_TASK=> new ExplorationCapsule(capsuleModel.dataProxy.get.panelUIData.coreObject.asInstanceOf[ExplorationTask])
      case BASIC_TASK=> new Capsule(capsuleModel.dataProxy.get.panelUIData.coreObject.asInstanceOf[Task])
      case CAPSULE=> new Capsule
    }
  }
  
  def buildTransition(sourceCapsule: IGenericCapsule, targetCapsule: IGenericCapsule,t: TransitionUI){
    sourceCapsule match {
      case x: IExplorationCapsule=> new ExplorationTransition(x,targetCapsule)
      case z: ICapsule => {
          t.transitionType match {
            case AGGREGATION=> new AggregationTransition(z,targetCapsule)
            case BASIC=> new Transition(z,targetCapsule) 
          }
        }
      case _=> throw new GUIUserBadDataError("No matching type for capsule " + sourceCapsule +". The transition can not be built")
    }
  }
}