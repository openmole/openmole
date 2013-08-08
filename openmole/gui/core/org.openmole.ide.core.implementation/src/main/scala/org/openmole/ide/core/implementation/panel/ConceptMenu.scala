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
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.builder.Builder
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.workflow.{ ISceneContainer, BuildMoleSceneContainer }
import scala.collection.JavaConversions._
import org.openmole.ide.core.implementation.factory.FactoryUI
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI
import org.openmole.ide.core.implementation.data._
import scala.swing.event.ButtonClicked
import scala.Some

object ConceptMenu {

  def createAndDisplaySamplingComposition(index: Int) = displayExtra(Builder.samplingCompositionUI(false), index)

  def createAndDisplayPrototype(index: Int) = displayExtra(Builder.prototypeUI, index)

  def createAndDisplayPrototype = display(Builder.prototypeUI)

  def createAndDisplayHook(index: Int) = displayExtra(Builder.hookUI(false), index)

  val menuItemMapping = new HashMap[DataProxyUI, MenuItem]()
  val mapping = new HashMap[List[String], Menu]

  def menu(seq: List[String]): Menu = {
    def menu0(seq: List[String], m: Menu): Menu = {
      if (seq.isEmpty) m
      else menu0(seq.tail, mapping.getOrElseUpdate(seq, {
        val child = new Menu(seq.head)
        m.contents += child
        child
      }))
    }
    menu0(seq.tail, mapping.getOrElseUpdate(List(seq.head), new Menu(seq.head)))
  }

  def menuItem[T](proxy: T, fact: FactoryUI, f: T ⇒ Unit): MenuItem = {
    if (fact.category.isEmpty) menuItem(proxy, fact.toString, f)
    else {
      val m = menu(fact.category)
      m.contents += menuItem(proxy, fact.toString, f)
      m
    }
  }

  def menuItem[T](proxy: T, s: String, f: T ⇒ Unit): MenuItem =
    new MenuItem(new Action(s) {
      override def apply = f(proxy)
    }) {
      listenTo(this)
      reactions += {
        case x: ButtonClicked ⇒
          publish(new ConceptChanged(this))
      }
    }

  def menuItem(f: ⇒ Unit): MenuItem = new MenuItem(new Action("New") {
    override def apply = f
  })

  val taskMenu = new PopupToolBarPresenter("Task", List(menuItem(display(Builder.taskUI(false)))), new Color(107, 138, 166))
  val environmentMenu = new PopupToolBarPresenter("Environment", List(menuItem(display(Builder.environmentUI(false)))), new Color(68, 120, 33))
  val prototypeMenu = new PopupToolBarPresenter("Prototype", List(menuItem(display(Builder.prototypeUI))), new Color(212, 170, 0))
  val samplingMenu = new PopupToolBarPresenter("Sampling", List(menuItem(display(Builder.samplingCompositionUI(false)))), new Color(255, 85, 85))
  val sourceMenu = new PopupToolBarPresenter("Source", List(menuItem(display(Builder.sourceUI(false)))), new Color(60, 60, 60))
  val hookMenu = new PopupToolBarPresenter("Hook", List(menuItem(display(Builder.hookUI(false)))), new Color(168, 120, 33))

  def factoryName(d: DataUI, factories: List[DataProxyFactory]): String = {
    List(factories.find { f ⇒ f.buildDataProxyUI.dataUI.getClass.isAssignableFrom(d.getClass) }).flatten.map {
      _.factory.toString
    }.headOption.getOrElse("AAAAAAAA")
  }

  def buildTaskMenu(f: TaskDataProxyUI ⇒ Unit, initDataUI: TaskDataUI) = {
    mapping.clear
    val factories = KeyRegistry.tasks.values.map {
      f ⇒ new TaskDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)

    val tmenu = factories.map {
      d ⇒ menuItem(d.buildDataProxyUI, d.factory, f)
    }

    new PopupToolBarPresenter(factoryName(initDataUI, factories), tmenu, new Color(107, 138, 166))
  }

  def buildEnvironmentMenu(f: EnvironmentDataProxyUI ⇒ Unit, initDataUI: EnvironmentDataUI) = {
    mapping.clear
    val factories = KeyRegistry.environments.values.map {
      f ⇒ new EnvironmentDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)

    val emenu = factories.map {
      d ⇒ menuItem(d.buildDataProxyUI, d.factory, f)
    }

    new PopupToolBarPresenter(factoryName(initDataUI, factories), emenu, new Color(68, 120, 33))
  }

  def buildPrototypeMenu(f: PrototypeDataProxyUI ⇒ Unit) = {
    mapping.clear
    val pmenu = (GenericPrototypeDataUI.base ::: GenericPrototypeDataUI.extra).sortBy(_.toString).map {
      d ⇒ menuItem(PrototypeDataProxyUI(d), d.toString, f)
    }
    new PopupToolBarPresenter("Prototype", pmenu, new Color(212, 170, 0))
  }

  def buildHookMenu(f: HookDataProxyUI ⇒ Unit, initDataUI: HookDataUI) = {
    mapping.clear
    val factories = KeyRegistry.hooks.values.map {
      f ⇒ new HookDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)
    val hmenu = factories.map {
      d ⇒ menuItem(d.buildDataProxyUI, d.factory, f)
    }
    new PopupToolBarPresenter(factoryName(initDataUI, factories), hmenu, new Color(168, 120, 33))
  }

  def buildSourceMenu(f: SourceDataProxyUI ⇒ Unit, initDataUI: SourceDataUI) = {
    mapping.clear
    val factories = KeyRegistry.sources.values.map {
      f ⇒ new SourceDataProxyFactory(f)
    }.toList.sortBy(_.factory.toString)
    val smenu = factories.map {
      d ⇒ menuItem(d.buildDataProxyUI, d.factory, f)
    }
    new PopupToolBarPresenter(factoryName(initDataUI, factories), smenu, new Color(60, 60, 60))
  }

  def -=(proxy: DataProxyUI) = {
    proxy match {
      case x: EnvironmentDataProxyUI         ⇒ environmentMenu.remove(menuItemMapping(proxy))
      case x: PrototypeDataProxyUI           ⇒ prototypeMenu.remove(menuItemMapping(proxy))
      case x: TaskDataProxyUI                ⇒ taskMenu.remove(menuItemMapping(proxy))
      case x: SamplingCompositionDataProxyUI ⇒ samplingMenu.remove(menuItemMapping(proxy))
      case x: HookDataProxyUI                ⇒ hookMenu.remove(menuItemMapping(proxy))
      case x: SourceDataProxyUI              ⇒ sourceMenu.remove(menuItemMapping(proxy))
    }
  }

  def +=(name: String, proxy: DataProxyUI) = addInMenu(proxy, addItem(name, proxy))

  def +=(proxy: DataProxyUI) = addInMenu(proxy, addItem(proxy))

  private def addInMenu(proxy: DataProxyUI, menuItem: MenuItem) = proxy match {
    case x: EnvironmentDataProxyUI         ⇒ environmentMenu.popup.contents += menuItem
    case x: PrototypeDataProxyUI           ⇒ prototypeMenu.popup.contents += menuItem
    case x: TaskDataProxyUI                ⇒ taskMenu.popup.contents += menuItem
    case x: SamplingCompositionDataProxyUI ⇒ samplingMenu.popup.contents += menuItem
    case x: HookDataProxyUI                ⇒ hookMenu.popup.contents += menuItem
    case x: SourceDataProxyUI              ⇒ sourceMenu.popup.contents += menuItem
  }

  def menuBar = new MenuBar {
    contents.append(prototypeMenu, taskMenu, samplingMenu, environmentMenu, hookMenu)
    minimumSize = new Dimension(size.width, 50)
  }

  def display(proxy: DataProxyUI) = {
    if (ScenesManager.tabPane.peer.getTabCount == 0) createTab(proxy)
    else ScenesManager.tabPane.selection.page.content match {
      case x: ISceneContainer ⇒ x.scene.displayPropertyPanel(proxy, 0)
      case _                  ⇒ createTab(proxy)
    }
  }

  def displayExtra(proxy: DataProxyUI,
                   index: Int) = {
    if (ScenesManager.tabPane.peer.getTabCount == 0) createTab(proxy)
    else ScenesManager.tabPane.selection.page.content match {
      case x: ISceneContainer ⇒ x.scene.displayPropertyPanel(proxy, index + 1)
      case _                  ⇒ createTab(proxy)
    }
  }

  def createTab(proxy: DataProxyUI) = DialogFactory.newTabName match {
    case Some(y: BuildMoleSceneContainer) ⇒
      y.scene.displayPropertyPanel(proxy, 0)
    case _ ⇒
  }

  def addItem(proxy: DataProxyUI): MenuItem = addItem(proxyName(proxy), proxy)

  def addItem(name: String,
              proxy: DataProxyUI): MenuItem = {
    menuItemMapping += proxy -> new MenuItem(new Action(proxyName(proxy)) {
      override def apply = {
        ConceptMenu.display(proxy)
      }
    })
    menuItemMapping(proxy)
  }

  def refreshItem(proxy: DataProxyUI) = {
    if (menuItemMapping.contains(proxy))
      menuItemMapping(proxy).action.title = proxyName(proxy)
  }

  def clearAllItems = {
    List(samplingMenu, prototypeMenu, taskMenu, environmentMenu, hookMenu, sourceMenu).foreach {
      _.removeAll
    }
    menuItemMapping.clear
  }

  def proxyName(proxy: DataProxyUI) =
    proxy.dataUI.name + (proxy match {
      case x: PrototypeDataProxyUI ⇒
        if (x.dataUI.dim > 0) " [" + x.dataUI.dim.toString + "]" else ""
      case _ ⇒ ""
    })

}
