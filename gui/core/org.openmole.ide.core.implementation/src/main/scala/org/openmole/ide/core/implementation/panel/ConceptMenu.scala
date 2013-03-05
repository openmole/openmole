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

import java.awt.Color
import java.awt.Dimension
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel.IComponentCategory
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy._
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.workflow.BuildMoleSceneContainer
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.core.implementation.builder.Builder
import scala.collection.JavaConversions._

object ConceptMenu {

  val menuItemMapping = new HashMap[IDataProxyUI, MenuItem]
  val mapping = new HashMap[IComponentCategory, Menu]

  def addCategoryComponents(rootComponent: IComponentCategory): Menu = {
    val menu = new Menu(rootComponent.name)
    mapping += rootComponent -> menu
    rootComponent.childs.foreach {
      cpt ⇒
        menu.contents += addCategoryComponents(cpt)
    }
    menu
  }

  val taskMenu = {
    addCategoryComponents(ComponentCategories.TASK)
    KeyRegistry.tasks.values.map {
      f ⇒ new TaskDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString).foreach {
      d ⇒
        mapping(d.factory.category).contents += new MenuItem(new Action(d.factory.toString) {
          override def apply = display(d.buildDataProxyUI, CREATION)
        })
    }
    new PopupToolBarPresenter("Task", mapping(ComponentCategories.TASK), new Color(107, 138, 166))
  }

  val environmentMenu = {
    addCategoryComponents(ComponentCategories.ENVIRONMENT)
    KeyRegistry.environments.values.map {
      f ⇒ new EnvironmentDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString).foreach {
      d ⇒
        mapping(ComponentCategories.ENVIRONMENT).contents += new MenuItem(new Action(d.factory.toString) {
          override def apply = display(d.buildDataProxyUI, CREATION)
        })
    }
    new PopupToolBarPresenter("Environment", mapping(ComponentCategories.ENVIRONMENT), new Color(68, 120, 33))
  }

  val prototypeMenu = {
    val menu = new MenuItem(new Action("New") {
      override def apply = display(new PrototypeDataProxyUI(GenericPrototypeDataUI[java.lang.Double], generated = false), CREATION)
    })
    new PopupToolBarPresenter("Prototype", menu, new Color(212, 170, 0))
  }

  val samplingMenu = {
    val menu = new MenuItem(new Action("New Composition") {
      override def apply = display(Builder.samplingCompositionUI(false), CREATION)
    })
    new PopupToolBarPresenter("Sampling", menu, new Color(255, 85, 85))
  }

  val hookMenu = {
    addCategoryComponents(ComponentCategories.HOOK)
    KeyRegistry.hooks.values.map {
      f ⇒ new HookDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString).foreach {
      d ⇒
        mapping(ComponentCategories.HOOK).contents += new MenuItem(new Action(d.factory.toString) {
          override def apply = display(d.buildDataProxyUI, CREATION)
        })
    }
    new PopupToolBarPresenter("Hook", mapping(ComponentCategories.HOOK), new Color(168, 120, 33))
  }

  def removeItem(proxy: IDataProxyUI) = {
    proxy match {
      case x: IEnvironmentDataProxyUI ⇒ environmentMenu.remove(menuItemMapping(proxy))
      case x: IPrototypeDataProxyUI ⇒ prototypeMenu.remove(menuItemMapping(proxy))
      case x: ITaskDataProxyUI ⇒ taskMenu.remove(menuItemMapping(proxy))
      case x: ISamplingCompositionDataProxyUI ⇒ samplingMenu.remove(menuItemMapping(proxy))
      case x: IHookDataProxyUI ⇒ hookMenu.remove(menuItemMapping(proxy))
    }
  }

  def menuBar = new MenuBar {
    contents.append(prototypeMenu, taskMenu, samplingMenu, environmentMenu, hookMenu)
    minimumSize = new Dimension(size.width, 50)
  }

  def display(proxy: IDataProxyUI,
              mode: Value) = {
    if (ScenesManager.tabPane.peer.getTabCount == 0) createTab(proxy, mode)
    else ScenesManager.tabPane.selection.page.content match {
      case x: ISceneContainer ⇒ x.scene.displayPropertyPanel(proxy, mode)
      case _ ⇒ createTab(proxy, mode)
    }
  }

  def createTab(proxy: IDataProxyUI,
                mode: Value) = DialogFactory.newTabName match {
    case Some(y: BuildMoleSceneContainer) ⇒ y.scene.displayPropertyPanel(proxy, mode)
    case _ ⇒
  }

  def addItem(proxy: IDataProxyUI): MenuItem = addItem(proxyName(proxy), proxy)

  def addItem(name: String,
              proxy: IDataProxyUI): MenuItem = {
    menuItemMapping += proxy -> new MenuItem(new Action(proxyName(proxy)) {
      override def apply = {
        ConceptMenu.display(proxy, EDIT)
      }
    })
    menuItemMapping(proxy)
  }

  def refreshItem(proxy: IDataProxyUI) = {
    if (menuItemMapping.contains(proxy))
      menuItemMapping(proxy).action.title = proxyName(proxy)
  }

  def clearAllItems = {
    List(samplingMenu, prototypeMenu, taskMenu, environmentMenu, hookMenu).foreach {
      _.removeAll
    }
    menuItemMapping.clear
  }

  def proxyName(proxy: IDataProxyUI) = {
    proxy.dataUI.name + (proxy match {
      case x: IPrototypeDataProxyUI ⇒
        if (x.dataUI.dim > 0) " [" + x.dataUI.dim.toString + "]" else ""
      case _ ⇒ ""
    })
  }

}