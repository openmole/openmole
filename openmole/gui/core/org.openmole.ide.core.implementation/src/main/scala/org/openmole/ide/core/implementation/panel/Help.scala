/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.ide.misc.widget.Helper
import scala.swing.{ Publisher, Component }
import scala.swing.event.FocusGained
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent

trait Help extends Publisher {
  lazy val help = new Helper

  def add(component: Component,
          h: org.openmole.ide.misc.widget.Help) = {
    help.add(component, h)
    listenTo(component)
  }

  reactions += {
    case FocusGained(source: Component, _, _)     ⇒ help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ help.switchTo(source)
  }
}