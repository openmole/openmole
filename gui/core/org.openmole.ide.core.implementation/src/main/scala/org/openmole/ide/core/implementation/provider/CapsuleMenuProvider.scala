/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import scala.swing.Action
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.action._
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.commons._
import scala.swing.CheckMenuItem
import scala.swing.Menu
import scala.swing.MenuItem
import org.openmole.ide.core.model.data.{ IHookDataUI, NoMemoryHook }
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import scala.swing.ScrollPane
import org.openmole.ide.core.implementation.builder._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.dialog.StatusBar

class CapsuleMenuProvider(scene: IMoleScene, capsule: ICapsuleUI) extends GenericMenuProvider {
  var taskMenu = new JMenu
  var itChangeCapsule = new Menu("to ")

  def initMenu = {
    items.clear
    if (ScenesManager.selection.size == 0) ScenesManager.addToSelection(capsule)
    val selectionSize = ScenesManager.selection.size
    val itStart = new JMenuItem("Define as starting capsule")
    val itIS = new JMenuItem("Add an input slot")
    val itRIS = new JMenuItem("Remove an input slot")
    val itR = new JMenuItem("Remove " + selectionSize + " capsule" + (if (selectionSize > 1) "s" else ""))
    val menuTask = new Menu("Task")

    itIS.addActionListener(new AddInputSlotAction(capsule))
    itR.addActionListener(new RemoveCapsuleAction(scene, capsule))
    itStart.addActionListener(new DefineMoleStartAction(scene, capsule))
    itRIS.addActionListener(new RemoveInputSlot(capsule))

    //Tasks
    Proxys.tasks.foreach {
      p ⇒
        menuTask.contents += new CheckMenuItem(p.dataUI.name) {
          action = new TaskEnvAction(p.dataUI.name, this) {
            def apply = {
              capsule.decapsule
              capsule.encapsule(p)
              selectOneItem(menuTask, item)
            }
          }
          capsule.dataUI.task match {
            case Some(t: ITaskDataProxyUI) ⇒
              selected = {
                p.dataUI.name == t.dataUI.name
              }
            case _ ⇒
          }
        }
    }

    menuTask.peer.insert(new CheckMenuItem("None") {
      action = new Action("None") {
        def apply = capsule.decapsule
      }
    }.peer, 0)
    items += (itIS, itRIS, itR, itStart, menuTask.peer)

    //Environments
    /* val menuEnv = new Menu("Environment")

    Proxys.environments.foreach {
      env ⇒
        menuEnv.contents += new CheckMenuItem(env.dataUI.name) {
          action = new TaskEnvAction(env.dataUI.name, this) {
            def apply = {
              capsule.setEnvironment(Some(env))
              selectOneItem(menuEnv, item)
            }
          }

          capsule.dataUI.environment match {
            case Some(e: IEnvironmentDataProxyUI) ⇒
              selected = {
                env.dataUI.name == e.dataUI.name
              }
            case _ ⇒ selected = false
          }
        }
    }

    menuEnv.peer.insert(new CheckMenuItem("None") {
      action = new Action("None") {
        def apply = capsule.setEnvironment(None)
      }
    }.peer, 0) */

    //Hooks
    /*val menuHook = new Menu("IHook")
    KeyRegistry.hooks.values.toList.sortBy {
      _.toString
    }.foreach {
      h ⇒
        menuHook.contents += new CheckMenuItem(h.toString) {
          selected = {
            if (capsule.dataUI.hooks.contains(h.coreClass)) {
              capsule.dataUI.hooks(h.coreClass).activated
            } else false
          }
          action = new HookAction(h, this)
        }
    }   */

    val menuBuilder = new Menu("Builder")
    KeyRegistry.builders.values.toList.sortBy {
      _.name
    }.foreach {
      b ⇒
        menuBuilder.contents += new MenuItem(b.name) {
          action = new Action(b.name) {
            def apply = Builder(scene, b, ScenesManager.selection.toList)
          }
        }
    }

    items += menuBuilder.peer
  }

  override def getPopupMenu(widget: Widget, point: Point) = {
    initMenu
    itChangeCapsule.peer.removeAll
    items -= itChangeCapsule.peer
    List(new MasterCapsuleType, new StrainerCapsuleType, new BasicCapsuleType).filterNot(_.getClass == capsule.dataUI.capsuleType.getClass).foreach {
      ctype ⇒
        itChangeCapsule.peer.add(new MenuItem(new ChangeCapsuleAction(capsule, ctype)).peer)
    }
    items += itChangeCapsule.peer
    super.getPopupMenu(widget, point)
  }

  def selectOneItem(menu: Menu, item: CheckMenuItem) =
    menu.contents.foreach {
      i ⇒
        i match {
          case mi: CheckMenuItem ⇒ mi.selected = false
        }
        item.selected = true
    }

  abstract class TaskEnvAction(name: String,
                               val item: CheckMenuItem) extends Action(name)

  /*class HookAction(factory: IHookFactoryUI,
                   it: CheckMenuItem) extends Action(factory.toString) {
    def apply = {
      if (!capsule.dataUI.hooks.contains(factory.coreClass))
        capsule.dataUI = capsule.dataUI :- factory.buildDataUI     */
  //  else capsule.dataUI.hooks(factory.coreClass).activated = it.selected
  //  capsule.hooked(if (capsule.dataUI.hooks.values.filter {
  //     _.activated
  //  }.size > 0) true
  //else false
  //  }
  //  }

}
