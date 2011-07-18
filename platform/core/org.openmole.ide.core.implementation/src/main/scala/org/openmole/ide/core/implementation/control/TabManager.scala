/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Component
import javax.swing.JTabbedPane
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.model.control.ITabManager

abstract class TabManager extends ITabManager{

  var tabbedPane: Option[JTabbedPane]= None
  var tabMap= new DualHashBidiMap[Object, Component]()
  
  override def removeTab(o: Object)= {
    o match {
      case o: Component=> { 
          tabbedPane.get.remove(o)
          tabMap.removeValue(o)
        }
      case _=>{
          tabbedPane.get.remove(tabMap.get(o))
          tabMap.remove(o)
        }
    }
  }
  
  def removeAllTabs= {
    tabbedPane.get.removeAll
    tabMap.clear
  }

  override def display: Unit = display(addTab)
    
  override def display(displayed: Object): Unit ={
    if (!tabMap.containsKey(displayed)) 
      addTab(displayed)
    tabbedPane.get.setSelectedComponent(tabMap.get(displayed))
  }
  
  def addMapping(obj: Object,comp: Component,name: String)= {
    tabMap.put(obj,comp)
    tabbedPane.get.add(name,tabMap.get(obj))
  }
  
  def setTabbedPane(tPane: JTabbedPane) = tabbedPane = Some(tPane)
  
  def getCurrentObject: Option[Object]= Some(tabMap.getKey(tabbedPane.get.getSelectedComponent))
  
}
//public abstract class TabManager implements ITabManager {
//
//    private BidiMap<Object, Component> tabMap = new DualHashBidiMap<Object, Component>();
//    private JTabbedPane tabbedPane;
//
//    @Override
//    public void removeTab(Object object){
//        tabbedPane.remove(tabMap.get(object));
//        tabMap.remove(object);
//    }
//    
//    @Override
//    public void removeTab(Component component){
//        tabbedPane.remove(component);
//        tabMap.removeValue(component);
//    }
//
//
//    @Override
//    public void removeAllTabs(){
//        tabbedPane.removeAll();
//        tabMap.clear();
//    }
//
//    @Override
//    public void display(Object displayed) {
//        if (!tabMap.containsKey(displayed)) {
//            addTab(displayed);
//        }
//        tabbedPane.setSelectedComponent(tabMap.get(displayed));
//    }
//
//    @Override
//    public void addMapping(Object obj,
//                           Component comp,
//                           String name) {
//        tabMap.put(obj, comp);
//        tabbedPane.add(name, tabMap.get(obj));
//    }
//
//    public void setTabbedPane(JTabbedPane tabbedPane) {
//        this.tabbedPane = tabbedPane;
//    }
//
//    public Object getCurrentObject(){
//        return tabMap.getKey(tabbedPane.getSelectedComponent());
//    }
//}
