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
import org.openmole.ide.core.workflow.implementation.CapsuleModelUI
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
  
  //def incrementCounter(entityType: String): String = Constants.simpleEntityName(entityType).toLowerCase + counters(entityType).addAndGet(1)
  
  def getName(entityType: String, increment: Boolean) = {
    if (increment) incrementCounter(entityType)
    else getDefaultName(entityType)
  } 
  
  def incrementCounter(entityType: String): String = entityType.toLowerCase + counters(entityType).addAndGet(1).toString
  
  def getDefaultName(entityType: String): String = entityType.toLowerCase + counters(entityType).toString
    
    
  def createCapsule(scene: MoleScene, locationPoint: Point): ICapsuleView = {
    val obUI = new CapsuleViewUI(scene,new CapsuleModelUI)          
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleView(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI.addInputSlot
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

//MoleScenesManager extends TabManager {
//
//    private static MoleScenesManager instance = null;
//    private Collection<IMoleScene> moleScenes = new ArrayList<IMoleScene>();
//    private Map<IMoleScene, Collection<Component>> childTabs = new HashMap<IMoleScene, Collection<Component>>();
//    private int count = 1;
//    private int nodeCounter = 0;
//    private boolean detailedView = false;
//
//    public void removeMoleScenes() {
//        moleScenes.clear();
//        removeAllTabs();
//    }
//
//    public void removeMoleScene(IMoleScene molescene) {
//        moleScenes.remove(molescene);
//        removeTab(molescene);
//    }
//
//    public void incrementNodeName() {
//        nodeCounter++;
//    }
//
//    public String getNodeName() {
//        return "task" + nodeCounter;
//    }
//
//    public void addMoleScene(IMoleScene ms) {
//        moleScenes.add(ms);
//        childTabs.put(ms, new ArrayList<Component>());
//    }
//
//    public IMoleScene addMoleScene(){
//        IMoleScene sc = new MoleScene();
//        addMoleScene(sc);
//        return sc;
//    }
//
//    public void addChild(IMoleScene sc,
//            Component co) {
//        childTabs.get(sc).add(co);
//    }
//
//    public Collection<IMoleScene> getMoleScenes() {
//        return moleScenes;
//    }
//
//    public void removeCurrentSceneAndChilds(IMoleScene curs) {
//        for (Component co : MoleScenesManager.getInstance().childTabs.get(curs)) {
//            TaskSettingsManager.getInstance().removeTab(co);
//        }
//        removeMoleScene(curs);
//    }
//
//    @Override
//    public void addTab(Object displayed) {
//        MoleScene scene = (MoleScene) displayed;
//
//        JComponent myView = scene.createView();
//        JScrollPane moleSceneScrollPane = new JScrollPane();
//        moleSceneScrollPane.setViewportView(myView);
//
//        String name;
//        if (scene.getManager().getName().equals("")) {
//            name = "Mole" + count;
//            count++;
//        } else {
//            name = scene.getManager().getName();
//        }
//        addMapping(displayed, moleSceneScrollPane, name);
//        scene.getManager().setName(name);
//    }
//
//     public void setDetailedView(boolean detailedView) {
//        this.detailedView = detailedView;
//    }
//
//    public boolean isDetailedView() {
//        return detailedView;
//    }
//
//    public static MoleScenesManager getInstance() {
//        if (instance == null) {
//            instance = new MoleScenesManager();
//        }
//        return instance;
//    }
//}
