/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.control

import java.awt.Component

trait ITabManager {
  def removeTab(o: Object)
  
  def removeAllTabs
  
  def display(displayed: Object)
  
  def display
  
  def addMapping(o: Object, c: Component, name: String)
  
  def addTab(displayed: Object): Object
  
  def addTab: Object
}

//    void removeTab(Object object);
//    void removeTab(Component component);
//    void removeAllTabs();
//    void display(Object displayed);
//    void addTab(Object displayed);
//    void addMapping(Object obj,Component comp,String name)