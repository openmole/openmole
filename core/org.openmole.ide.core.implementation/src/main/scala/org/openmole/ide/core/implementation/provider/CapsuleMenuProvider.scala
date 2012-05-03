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
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.action._

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
        val menuTask = new JMenu("Set task")

        itIS.addActionListener(new AddInputSlotAction(capsule))
        itR.addActionListener(new RemoveCapsuleAction(scene, capsule))
        itStart.addActionListener(new DefineMoleStartAction(scene, capsule))
        itRIS.addActionListener(new RemoveInputSlot(capsule))

        Proxys.tasks.foreach { p ⇒
          menuTask.add(new JMenuItem(new Action(p.dataUI.name) {
            override def apply = {
              capsule.encapsule(p)
            }
          }.peer))
        }
        menuTask.insert(new JMenuItem(new Action("None") {
          override def apply = capsule.decapsule
        }.peer), 0)
        items += (itIS, itRIS, itR, itStart, menuTask)
      case _ ⇒
    }

    val menuEnv = new JMenu("Set environment")

    Proxys.environments.foreach { env ⇒
      menuEnv.add(new JMenuItem(new Action(env.dataUI.name) {
        override def apply = capsule.setEnvironment(Some(env))
      }.peer))
    }
    menuEnv.insert(new JMenuItem(new Action("None") {
      override def apply = capsule.setEnvironment(None)
    }.peer), 0)

    items += menuEnv
  }

  def addTaskMenus = encapsulated = true

  override def getPopupMenu(widget: Widget, point: Point) = {
    initMenu
    super.getPopupMenu(widget, point)
  }
}