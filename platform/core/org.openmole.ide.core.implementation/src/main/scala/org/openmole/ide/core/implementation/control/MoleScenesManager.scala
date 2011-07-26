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

import java.awt.Component
import java.awt.Point
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JScrollPane
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.misc.exception.GUIUserBadDataError
import javax.swing.JTabbedPane
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.workflow.CapsuleUI
import org.openmole.ide.core.implementation.workflow.ExecutionMoleScene
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.swing.ScrollPane
import scala.collection.JavaConversions._
import scala.swing.TabbedPane

object MoleScenesManager{

  var detailedView= false
  var countMole= 0
  var countExec= 0
  var moleScenes= new HashMap[BuildMoleScene, ListBuffer[ExecutionMoleScene]] 
  var sceneTabs = new DualHashBidiMap[IMoleScene,TabbedPane.Page]
  var tabbedPane= new TabbedPane
  
  def currentScene : BuildMoleScene = {
    sceneTabs.getKey(tabbedPane.selection.page) match {
      case m: BuildMoleScene => m
      case _=> throw new GUIUserBadDataError("The current scene is not a mole view, please first select a mole before build it")
    }
  }
  
  def createCapsule(scene: BuildMoleScene, locationPoint: Point): ICapsuleUI = {
    val obUI = new CapsuleUI(scene)
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleUI(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI    
  }

  def removeMoleScenes= {
    moleScenes.clear
    tabbedPane.peer.removeAll
  }
  
  def removeMoleScene(ms: BuildMoleScene)= {
    moleScenes.remove(ms)
    tabbedPane.pages-=sceneTabs.get(ms)
  }
    
  def addBuildMoleScene(ms: BuildMoleScene): BuildMoleScene = {
    moleScenes.getOrElseUpdate(ms, new ListBuffer[ExecutionMoleScene])
    addTab(ms,{countMole+= 1; "Mole"+countMole},new ScrollPane {peer.setViewportView(ms.createView)})
    ms
  }
    
  def addBuildMoleScene: BuildMoleScene = addBuildMoleScene(new BuildMoleScene)
    
  def addExecutionMoleScene(bms : BuildMoleScene): ExecutionMoleScene = {
    val ms = new ExecutionMoleScene
    moleScenes.getOrElseUpdate(bms, new ListBuffer[ExecutionMoleScene]) += ms
    addTab(ms,{countExec+= 1; "Exec"+countExec},new ScrollPane {peer.setViewportView(ms.createView)})
    ms
  }
  
  def removeCurrentSceneAndChilds= {
    sceneTabs.getKey(tabbedPane.selection.page) match {
      case bms: BuildMoleScene => {moleScenes(bms).foreach(ems=>tabbedPane.pages-= sceneTabs.get(ems))
                                   removeMoleScene(bms)}
      case ems: ExecutionMoleScene=> tabbedPane.pages-=sceneTabs.get(ems)
    }
  }
  
  def addTab(scene: MoleScene,n: String,sp: ScrollPane): IMoleScene = {
    val name= scene.manager.name.getOrElse(n)
    val p = new TabbedPane.Page(name,sp)
    tabbedPane.pages += p
    sceneTabs.put(scene,p)
    scene.manager.name= Some(name)
    scene
  }
  
  def displayBuildMoleScene: Unit = displayBuildMoleScene(addBuildMoleScene)

  def displayBuildMoleScene(displayed: BuildMoleScene): Unit = {
    if (!sceneTabs.containsKey(displayed)) addBuildMoleScene
    tabbedPane.selection.page= sceneTabs.get(displayed)
  }

  def displayExecutionMoleScene(displayed: ExecutionMoleScene) = tabbedPane.selection.page= sceneTabs.get(displayed)
}