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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import scala.swing.Action
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.action._
import org.openmole.ide.core.implementation.registry.KeyRegistry
import scala.swing.CheckMenuItem

class CapsuleMenuProvider(scene: IMoleScene, capsule: ICapsuleUI) extends GenericMenuProvider {
  var encapsulated = false
  var taskMenu = new JMenu

  def initMenu = {
    items.clear

    scene match {
      case x: BuildMoleScene ⇒
        val itStart = new JMenuItem("Define as starting capsule")
        val itIS = new JMenuItem("Add an input slot")
        val itRIS = new JMenuItem("Remove an input slot")
        val itR = new JMenuItem("Remove capsule")
        val menuTask = new JMenu("Task")

        itIS.addActionListener(new AddInputSlotAction(capsule))
        itR.addActionListener(new RemoveCapsuleAction(scene, capsule))
        itStart.addActionListener(new DefineMoleStartAction(scene, capsule))
        itRIS.addActionListener(new RemoveInputSlot(capsule))

        Proxys.tasks.foreach { p ⇒
          menuTask.add(new CheckMenuItem(p.dataUI.name) {
            action = new Action(p.dataUI.name) {
              def apply = {
                capsule.encapsule(p)
              }
            }
          }.peer)
        }
        menuTask.insert(new CheckMenuItem("None") {
          action = new Action("None") {
            def apply = capsule.decapsule
          }
        }.peer, 0)
        items += (itIS, itRIS, itR, itStart, menuTask)
      case _ ⇒
    }

    val menuEnv = new JMenu("Environment")

    Proxys.environments.foreach { env ⇒
      menuEnv.add(new CheckMenuItem(env.dataUI.name) {
        action = new Action(env.dataUI.name) {
          def apply = capsule.setEnvironment(Some(env))
        }
        capsule.dataUI.environment match {
          case e: IEnvironmentDataProxyUI ⇒
            println("END :: " + env.dataUI.name + { env.dataUI.name == e.dataUI.name })
            selected = { env.dataUI.name == e.dataUI.name }
          case _ ⇒
        }
      }.peer)
    }
    menuEnv.insert(new CheckMenuItem("None") {
      action = new Action("None") {
        def apply = capsule.setEnvironment(None)
      }
    }.peer, 0)

    val menuHook = new JMenu("Hook")
    KeyRegistry.hooks.values.toList.sortBy { _.toString }.foreach { h ⇒
      menuHook.add(new CheckMenuItem(h.toString) {
        selected = {
          if (capsule.dataUI.hooks.contains(h.coreClass))
            capsule.dataUI.hooks(h.coreClass).activated
          else false
        }
        action = new HookAction(h, this)
      }.peer)
      items += (menuEnv, menuHook)
    }
  }

  def addTaskMenus = encapsulated = true

  override def getPopupMenu(widget: Widget, point: Point) = {
    initMenu
    super.getPopupMenu(widget, point)
  }

  class HookAction(factory: IHookFactoryUI,
                   it: CheckMenuItem) extends Action(factory.toString) {
    def apply = {
      if (!capsule.dataUI.hooks.contains(factory.coreClass))
        capsule.dataUI.hooks += factory.coreClass -> factory.buildDataUI
      else capsule.dataUI.hooks(factory.coreClass).activated = it.selected
    }
  }
}
