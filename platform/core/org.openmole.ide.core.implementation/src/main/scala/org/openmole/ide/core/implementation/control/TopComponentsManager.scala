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

import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.palette.PaletteSupport
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.model.commons.MoleSceneType._
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.openmole.ide.misc.exception.GUIUserBadDataError
import scala.collection.JavaConversions._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.workspace.Workspace

object TopComponentsManager {

  var detailedView= false
  var countMole= 0
  var countExec= 0
  var topComponents= new HashMap[MoleSceneTopComponent, ListBuffer[MoleSceneTopComponent]] 
  var executionTabs = new HashMap[IMoleScene,ExecutionManager]
  EventDispatcher.listen(Workspace.instance, new PasswordListener, classOf[Workspace.PasswordRequired])
  
  def moleScenes = topComponents.keys.map(_.getMoleScene)
  
  def removeTopComponent(bs: MoleSceneTopComponent) = topComponents-= bs
  
  def removeAllExecutionTopComponent(tc: MoleSceneTopComponent) = {
    topComponents(tc).foreach(_.close)
    topComponents(tc).clear
  }
  
  def buildMoleScene = {
    val sc = new MoleScene(BUILD)
    sc.manager.name = Some({countMole+= 1; "Mole"+countMole})
    sc
  }
  
  def addTopComponent:MoleSceneTopComponent = addTopComponent(new MoleSceneTopComponent)
    
  def addTopComponent(tc: MoleSceneTopComponent): MoleSceneTopComponent= {
    tc.open
    tc
  }
  
  def executionManager(execution: IMoleExecution) = executionTabs.values.filter(_.moleExecution==execution).head
    
  def currentExecutionManager: ExecutionManager = executionTabs.getOrElse(PaletteSupport.currentMoleSceneTopComponent.get.getMoleScene,
                                                                          throw new GUIUserBadDataError("This Mole can not be run. Pleas build it first"))
  
  def registerTopComponent(tc: MoleSceneTopComponent) = topComponents.getOrElseUpdate(tc, new ListBuffer[MoleSceneTopComponent])
  
  def addExecutionTopComponent(tc : MoleSceneTopComponent): MoleSceneTopComponent = {
    val clone = tc.getMoleScene.copy
    clone.manager.name = Some({countExec+= 1; tc.getMoleScene.manager.name.get+"_"+countExec})
    val ntc = new MoleSceneTopComponent(clone)
    topComponents(tc)+= ntc
    ntc.open
    ntc
  }
  
  def displayExecutionView(ms:IMoleScene) {
    if (ms.moleSceneType == EXECUTION) {
      val (mole, prototypeMapping,capsuleMapping) = MoleMaker.buildMole(ms.manager)
      ExecutionSupport.changeView(executionTabs.getOrElseUpdate(ms, new ExecutionManager(ms.manager)))
    }
  }
}