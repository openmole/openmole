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
import org.openide.windows.TopComponent
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import scala.collection.JavaConversions._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.core.implementation.dialog.DialogFactory

object TopComponentsManager {

  var countExec = new AtomicInteger  
  var currentMoleSceneTopComponent: Option[MoleSceneTopComponent] = None  
  var connectMode = true
  PasswordListner.apply
  
  def closeOpenedTopComponents = {
    currentMoleSceneTopComponent match {
      case Some(x: MoleSceneTopComponent)=>x.getOpened.foreach(_.close)
      case _=>
    }
  }
  
  def setCurrentMoleSceneTopComponent(ms: MoleSceneTopComponent) = currentMoleSceneTopComponent = Some(ms)
  
  def moleScenes = topComponents.map{_.getMoleScene}
  
  def topComponents = TopComponent.getRegistry.getOpened.
  filter(_.isInstanceOf[MoleSceneTopComponent]).
  map{_.asInstanceOf[MoleSceneTopComponent]}
    
  def setDetailedView(b: Boolean) = moleScenes.foreach{ ms=>
    ms.manager.capsules.values.foreach{c=>
      c.detailedView = b
      c.connectableWidget.setDetailedView}
    ms.validate
    ms.refresh}
  
  def addTopComponent: MoleSceneTopComponent = addTopComponent("")
  
  def addTopComponent(name: String): MoleSceneTopComponent = addTopComponent(new BuildMoleScene(name))
  
  def addTopComponent(ms: BuildMoleScene): MoleSceneTopComponent= {
    val mc= new BuildMoleComponent(ms)
    mc.moleSceneTopComponent.open
    mc.moleSceneTopComponent.requestActive
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