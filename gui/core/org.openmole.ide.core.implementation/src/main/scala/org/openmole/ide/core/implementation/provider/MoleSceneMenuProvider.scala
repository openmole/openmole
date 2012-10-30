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
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.action.AddCapsuleAction
import org.openmole.ide.core.implementation.action.AddTaskAction
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.swing.Action
import scala.swing.MenuItem

class MoleSceneMenuProvider(moleScene: IMoleScene) extends GenericMenuProvider {

  def initMenu = {
    val itemCapsule = new MenuItem(new AddCapsuleAction(moleScene, this))
    items += itemCapsule.peer
  }

  override def getPopupMenu(widget: Widget,
                            point: Point) = {
    items.clear
    initMenu
    Proxys.tasks.foreach { p ⇒ items += new JMenuItem(new AddTaskAction(moleScene, p, this).peer)
    }
    val itPaste = new MenuItem(new Action("Paste") { def apply = ScenesManager.pasteCapsules(moleScene, point) })
    itPaste.enabled = !ScenesManager.selection.isEmpty
    items.insert(0, itPaste.peer)
    super.getPopupMenu(widget, point)
  }
}