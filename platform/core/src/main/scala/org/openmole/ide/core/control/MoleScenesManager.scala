/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.control

import java.awt.Component
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JScrollPane
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.model.IMoleScene
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object MoleScenesManager extends TabManager{

  var detailedView= false
//  var nodeCounter= 0
//  var samplingCounter= 0
//  var prototypeCounter= 0
//  var environmentCounter= 0
  var count= 0
  var moleScenes= HashSet.empty[IMoleScene]
  var childTabs= new HashMap[IMoleScene, HashSet[Component]]
  val counters = Map[String,AtomicInteger](Constants.TASK_MODEL-> new AtomicInteger(0), 
                                           Constants.PROTOTYPE_MODEL-> new AtomicInteger(0), 
                                           Constants.ENVIRONMENT_MODEL-> new AtomicInteger(0),
                                           Constants.SAMPLING_MODEL-> new AtomicInteger(0))
  
  def incrementCounter(entityType: String): String = {
    Constants.simpleEntityName(entityType).toLowerCase + counters(entityType).addAndGet(1)
  }

//  def incrementTaskName: String = {
//    nodeCounter+= 1
//    "task" + nodeCounter
//  }
//  
//  def incrementPrototypeName: String = {
//    prototypeCounter += 1
//    "prototype" + prototypeCounter
//  }
//  
//  def incrementSamplingName: String = {
//    samplingCounter+= 1
//    "sampling" + samplingCounter
//  }
//  
//  def incrementEnvironmentName: String = {
//    environmentCounter+= 1
//    "prototype" + prototypeCounter
//  }
  
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
