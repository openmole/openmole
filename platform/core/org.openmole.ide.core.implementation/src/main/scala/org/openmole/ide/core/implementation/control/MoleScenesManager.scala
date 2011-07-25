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
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WeakHashMap
import scala.swing.ScrollPane
import scala.collection.JavaConversions._

object MoleScenesManager{

  var detailedView= false
  var count= 0
  var moleScenes= new HashMap[BuildMoleScene, ListBuffer[ExecutionMoleScene]] 
  var sceneTabs = new DualHashBidiMap[IMoleScene,ScrollPane]
  var tabbedPane: Option[JTabbedPane]= None
  
  def setTabbedPane(tp: JTabbedPane) = tabbedPane = Some(tp)
  
  def createCapsule(scene: BuildMoleScene, locationPoint: Point): ICapsuleUI = {
    val obUI = new CapsuleUI(scene)
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleUI(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI    
  }

  def removeMoleScenes= {
    moleScenes.clear
    tabbedPane.get.removeAll
  }
  
  def removeMoleScene(ms: BuildMoleScene)= {
    moleScenes.remove(ms)
    tabbedPane.get.remove(sceneTabs.get(ms).peer)
  }
  def addBuildMoleScene(ms: BuildMoleScene): BuildMoleScene = {
    moleScenes.getOrElseUpdate(ms, new ListBuffer[ExecutionMoleScene])
    addTab(ms)
    ms
  }
    
  def addBuildMoleScene: BuildMoleScene = addBuildMoleScene(new BuildMoleScene)
    
  def addExecutionMoleScene(bms : BuildMoleScene): IMoleScene = {
    val ms = new ExecutionMoleScene
    moleScenes.getOrElseUpdate(bms, new ListBuffer[ExecutionMoleScene]) += ms
    ms
  }
  
  def removeCurrentSceneAndChilds= {
    tabbedPane.get.getSelectedComponent match {
      case bms: BuildMoleScene => {moleScenes(bms).foreach(ems=>tabbedPane.get.remove(sceneTabs.get(ems).peer))
                                   removeMoleScene(bms)}
      case ems: ExecutionMoleScene=> tabbedPane.get.remove(sceneTabs.get(ems).peer)
    }
  }
  
  def addTab(scene: MoleScene): IMoleScene = {
    val name= scene.manager.name.getOrElse({count+= 1; "Mole"+count})
    sceneTabs.put(scene, {var sp = new ScrollPane
                          sp.peer.setViewportView(scene.createView)
                          tabbedPane.get.add(name,sp.peer)
                          sp})
    scene.manager.name= Some(name)
    scene
  }
  
  def displayBuildMoleScene: Unit = displayBuildMoleScene(addBuildMoleScene)

  def displayBuildMoleScene(displayed: BuildMoleScene): Unit ={
    if (!sceneTabs.containsKey(displayed)) addBuildMoleScene
    sceneTabs.foreach(t=>println("::: "+t))
    tabbedPane.get.setSelectedComponent(sceneTabs.get(displayed).peer)
  }

}