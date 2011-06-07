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

package org.openmole.ide.core.control

import java.awt.Component
import java.awt.Point
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JScrollPane
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.model.ICapsuleView
import org.openmole.ide.core.workflow.model.IMoleScene
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

object MoleScenesManager extends TabManager{

  var detailedView= false
  var count= 0
  var moleScenes= HashSet.empty[IMoleScene]
  var childTabs= new HashMap[IMoleScene, HashSet[Component]]
  val counters = Map[String,AtomicInteger](Constants.TASK-> new AtomicInteger(0), 
                                           Constants.PROTOTYPE-> new AtomicInteger(0), 
                                           Constants.ENVIRONMENT-> new AtomicInteger(0),
                                           Constants.SAMPLING-> new AtomicInteger(0))
   
  def incrementCounter(entityType: String): String = entityType.toLowerCase + counters(entityType).addAndGet(1).toString  
    
  def createCapsule(scene: MoleScene, locationPoint: Point): ICapsuleView = {
    val obUI = new CapsuleViewUI(scene)
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleView(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI    
  }

  def removeMoleScenes= {
    moleScenes.clear
    removeAllTabs
  }
  
  def removeMoleScene(moleScene: IMoleScene)= {
    moleScenes.remove(moleScene)
    removeTab(moleScene)
  }
  
  def addMoleScene(ms: IMoleScene): IMoleScene = {
    moleScenes+= ms
    childTabs+= ms-> HashSet.empty[Component]
    ms
  }
  
  def addMoleScene: IMoleScene = addMoleScene(new MoleScene)
  
  def addChild(sc: IMoleScene, co: Component)= childTabs(sc)+= co
  
  def removeCurrentSceneAndChilds(curs: IMoleScene)= {
    childTabs(curs).foreach(TaskSettingsManager.removeTab(_))
    removeMoleScene(curs)
  }
  
  override def addTab: IMoleScene = addTab(addMoleScene)
  
  override def addTab(displayed: Object): IMoleScene = {
    val scene= displayed.asInstanceOf[MoleScene]
    val myView = scene.createView
    val moleSceneScrollPane = new JScrollPane
    moleSceneScrollPane.setViewportView(myView)
    
    val name= scene.manager.name.getOrElse({count+= 1; "Mole"+count})
    addMapping(displayed,moleSceneScrollPane, name)
    scene.manager.name= Some(name)
    scene
  }
}