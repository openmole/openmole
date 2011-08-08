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

import java.awt.Point
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.model.commons.MoleSceneType._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.swing.ScrollPane
import scala.collection.JavaConversions._

object MoleScenesManager{

  var detailedView= false
  var countMole= 0
  var countExec= 0
  var moleScenes= new HashMap[IMoleScene, ListBuffer[IMoleScene]] 
    
  def createCapsule(caps: ICapsuleUI,scene: IMoleScene, locationPoint: Point): ICapsuleUI = {
    scene.initCapsuleAdd(caps)
    scene.manager.registerCapsuleUI(caps)
    scene.graphScene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    caps   
  }
  
  def createCapsule(scene: IMoleScene, locationPoint: Point): ICapsuleUI = createCapsule(new CapsuleUI(scene),scene, locationPoint)
    
  def createEdge(scene: IMoleScene,s: ICapsuleUI, t:IInputSlotWidget,transitionType: TransitionType.Value,cond: Option[String]) = {
    if (scene.manager.registerTransition(s, t, transitionType,cond))
      scene.createEdge(scene.manager.capsuleID(s), scene.manager.capsuleID(t.capsule))  
  }
  
  def removeMoleScenes= {
    moleScenes.clear
    TabManager.removeAllSceneTabs
  }
  
  def removeMoleScene(ms: IMoleScene)= {
    moleScenes.remove(ms)
    TabManager.removeSceneTab(ms)
  }
    
  def addBuildMoleScene(ms: IMoleScene): IMoleScene = {
    moleScenes.getOrElseUpdate(ms, new ListBuffer[IMoleScene])
    TabManager.addTab(ms,{countMole+= 1; "Mole"+countMole},new ScrollPane {peer.setViewportView(ms.graphScene.createView)})
    ms
  }
    
  def addBuildMoleScene: IMoleScene = addBuildMoleScene(new MoleScene(BUILD))
    
  def addExecutionMoleScene(bms : IMoleScene): IMoleScene = {
    val ms = bms.copy
    moleScenes.getOrElseUpdate(bms, new ListBuffer[IMoleScene]) += ms
    TabManager.addTab(ms,{countExec+= 1; "Exec"+countExec},new ScrollPane {peer.setViewportView(ms.graphScene.createView)})
    ms
  }
  
  def removeCurrentSceneAndChilds= {
    val ms = TabManager.currentScene
    ms.moleSceneType match {
      case BUILD => {moleScenes(ms).foreach(TabManager.removeSceneTab(_))
                     removeMoleScene(ms)}
      case EXECUTION=> { TabManager.removeSceneTab(ms)
        }
    }
  }

}