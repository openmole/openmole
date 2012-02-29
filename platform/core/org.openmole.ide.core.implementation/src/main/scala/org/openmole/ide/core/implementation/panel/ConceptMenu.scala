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
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.EnvironmentDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.SamplingDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyFactory
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.commons.Constants._
import scala.collection.JavaConversions._

object ConceptMenu {

  val menuItemMapping = new HashMap[IDataProxyUI,MenuItem]
    
  val environementClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[IEnvironmentFactoryUI]).map{f=>new EnvironmentDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => environementClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
    
  val environmentMenu = new PopupToolBarPresenter("Environments", environementClasses)
  
  val taskClasses = new Menu("New") 
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).map{f=>new TaskDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => taskClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  val taskMenu = new PopupToolBarPresenter("Tasks", taskClasses)
  
  
  val prototypeClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[IPrototypeFactoryUI[_]]).map{f=>new PrototypeDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => prototypeClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  val prototypeMenu = new PopupToolBarPresenter("Prototypes", prototypeClasses)
  
  
  val samplingClasses = new Menu("New")
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).map{f=>new SamplingDataProxyFactory(f)}.toList.sortBy(_.factory.displayName).foreach(
    d => samplingClasses.contents += new MenuItem(new Action(d.factory.displayName){
        override def apply = {
          val proxy = d.buildDataProxyUI
          display(proxy,CREATION)
        }}))
  
  
  val samplingMenu = new PopupToolBarPresenter("Samplings", samplingClasses)
  
  def removeItem(proxy: IDataProxyUI) = {
    proxy match {
      case x:IEnvironmentDataProxyUI=> environmentMenu.remove(menuItemMapping(proxy))
      case x:IPrototypeDataProxyUI=> prototypeMenu.remove(menuItemMapping(proxy))
      case x:ITaskDataProxyUI=> taskMenu.remove(menuItemMapping(proxy))
      case x:ISamplingDataProxyUI=> samplingMenu.remove(menuItemMapping(proxy))
    }
  } 
  
  def menuBar = new MenuBar{
    contents.append(prototypeMenu,taskMenu,samplingMenu,environmentMenu)
    minimumSize = new Dimension(size.width,50)
  }
        
  def display(proxy: IDataProxyUI,
              mode: Value) = 
                TopComponentsManager.currentMoleSceneTopComponent match {
      case Some(x: MoleSceneTopComponent)=> x.getMoleScene.displayPropertyPanel(proxy, mode)
      case None=> DialogFactory.newTabName match {
          case Some(x: MoleSceneTopComponent)=> x.getMoleScene.displayPropertyPanel(proxy, mode)
          case None=>
        }
    }
  
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
  
  def clearAllItems = {
    List(samplingMenu,prototypeMenu,taskMenu,environmentMenu).foreach{_.removeAll}
    menuItemMapping.clear
  }
}