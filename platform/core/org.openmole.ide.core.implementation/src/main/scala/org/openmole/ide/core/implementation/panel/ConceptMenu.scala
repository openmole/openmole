/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.panel

import java.awt.Dimension
import org.openide.util.Lookup
import org.openmole.ide.core.model.factory.IPrototypeFactoryUI
import org.openmole.ide.core.model.factory.ISamplingFactoryUI
import org.openmole.ide.core.model.factory.ITaskFactoryUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.factory.IEnvironmentFactoryUI
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.EnvironmentDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.SamplingDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyFactory
import org.openmole.ide.core.model.commons.Constants._
import scala.collection.JavaConversions._

object ConceptMenu {

  val menuItemMapping = new HashMap[IDataProxyUI,MenuItem]
  
  val environmentMenu = new Menu("Environment")
  val environementClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[IEnvironmentFactoryUI]).map{f=>new EnvironmentDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => environementClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
    
  environmentMenu.contents += environementClasses
  
  val taskMenu = new Menu("Task")
  val taskClasses = new Menu("New") 
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).map{f=>new TaskDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => taskClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  taskMenu.contents += taskClasses
  
  
  val prototypeMenu = new Menu("Prototype")
  val prototypeClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[IPrototypeFactoryUI[_]]).map{f=>new PrototypeDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => prototypeClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  prototypeMenu.contents += prototypeClasses
  
  
  val samplingMenu = new Menu("Sampling")
  val samplingClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).map{f=>new SamplingDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => samplingClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  samplingMenu.contents += samplingClasses
  
  def removeItem(proxy: IDataProxyUI) = {
    proxy match {
      case x:IEnvironmentDataProxyUI=> environmentMenu.peer.remove(menuItemMapping(proxy).peer)
      case x:IPrototypeDataProxyUI=> prototypeMenu.peer.remove(menuItemMapping(proxy).peer)
      case x:ITaskDataProxyUI=> taskMenu.peer.remove(menuItemMapping(proxy).peer)
      case x:ISamplingDataProxyUI=> samplingMenu.peer.remove(menuItemMapping(proxy).peer)
    }
  } 
  
  def menuBar = new MenuBar{
    contents.append(prototypeMenu,taskMenu,samplingMenu,environmentMenu)
    minimumSize = new Dimension(size.width,50)
  }
        
  def display(proxy: IDataProxyUI,
              mode: Value) = 
                TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.displayPropertyPanel(proxy,mode)
  
  def addItem(proxy: IDataProxyUI): MenuItem = addItem(proxy.dataUI.name,proxy)
  
  def addItem(name: String,
              proxy: IDataProxyUI) : MenuItem = {
    menuItemMapping += proxy-> new MenuItem( new Action(proxy.dataUI.name) {
        override def apply = {
          ConceptMenu.display(proxy,EDIT)
        }
      })
    menuItemMapping(proxy)
  }
  
  def refreshItem(proxy: IDataProxyUI) = {
    if (menuItemMapping.contains(proxy))
      menuItemMapping(proxy).action.title = proxy.dataUI.name
  }
}