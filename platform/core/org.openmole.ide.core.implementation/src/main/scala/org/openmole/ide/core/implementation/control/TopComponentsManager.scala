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

package org.openmole.ide.core.implementation.control

import org.openmole.ide.core.model.control.IMoleComponent
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.core.implementation.dialog.DialogFactory

object TopComponentsManager {

  var detailedView= false
  var countExec= new AtomicInteger
  var topComponents= new HashSet[BuildMoleComponent] 
  EventDispatcher.listen(Workspace.instance, new PasswordListener, classOf[Workspace.PasswordRequired])
  
  def moleScenes = topComponents.map{_.moleScene}
  
  def removeTopComponent(mc: IMoleComponent) = {
    mc match {
      case x: BuildMoleComponent=> topComponents-= x
      case _=>
    }
  }
    
  
  def addTopComponent:MoleSceneTopComponent = addTopComponent(new BuildMoleScene(DialogFactory.newTabName))
  
  def addTopComponent(ms: BuildMoleScene): MoleSceneTopComponent= {
    val mc= new BuildMoleComponent(ms)
    topComponents+= mc
    mc.moleSceneTopComponent.open
    mc.moleSceneTopComponent.setVisible(true)
    mc.moleSceneTopComponent
  }
    
  def addExecutionTopComponent(mc : IMoleComponent): MoleSceneTopComponent = {
    mc match {
      case x:BuildMoleComponent=>
        MoleMaker.buildMole(x.moleScene.manager) // test wether the mole can be built
        val clone = x.moleScene.copy
        clone.manager.name = Some({ x.moleScene.manager.name.get+"_"+countExec.incrementAndGet})
        val ecomp = new ExecutionMoleComponent(clone)
        x.executionMoleSceneComponents += ecomp
        ecomp.moleSceneTopComponent.open
        ecomp.moleSceneTopComponent
    }
  }
  
  def stopAndCloseExecutions(mc: IMoleComponent) = {
    mc match {
      case x: BuildMoleComponent=>x.stopAndCloseExecutions
      case _=>
    }
  }
  
  def displayExecutionView(tc:IMoleComponent) {
    tc match {
      case x:ExecutionMoleComponent=>
        ExecutionSupport.changeView(x.executionManager)
      case _=>
    }
  }
}