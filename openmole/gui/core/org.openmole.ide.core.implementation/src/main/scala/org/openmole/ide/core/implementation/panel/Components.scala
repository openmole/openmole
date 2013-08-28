/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

import scala.swing.{ Publisher, TabbedPane, Component, Alignment }
import org.openmole.ide.misc.widget.PluginPanel

trait Components extends Publisher with Help {

  def components: List[(String, Component)]

  def tabbedPane: TabbedPane = tabbedPane(components)

  def tabbedPane(ioPage: (String, Component)): TabbedPane = {
    if (components.isEmpty) tabbedPane(List(ioPage))
    else tabbedPane((List(components.head) :+ ioPage) ::: components.tail)
  }

  def tabbedPane(compts: List[(String, Component)]): TabbedPane = new TabbedPane {
    compts.foreach {
      c ⇒ pages += new TabbedPane.Page(c._1, c._2)
    }
    if (pages.size > 5) this.tabPlacement = Alignment.Left
  }

  def panel = new PluginPanel("wrap") {
    components.foreach {
      case (k, v) ⇒ contents += v
    }
  }

  def bestDisplay = {
    if (components.size == 1) panel
    else tabbedPane
  }
}