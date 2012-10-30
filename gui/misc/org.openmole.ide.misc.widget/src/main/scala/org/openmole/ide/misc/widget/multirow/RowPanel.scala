/*
 * Copyright (C) 2011 Mathieu Leclaire
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

package org.openmole.ide.misc.widget.multirow

import javax.swing.JPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing._

class RowPanel[T](val components: List[Component],
                  val plusAllowed: Plus = ADD) extends PluginPanel("wrap,insets -2 5 -2 5") with IRowPanel {
  var extendedPanel: Option[JPanel] = None

  contents += new PluginPanel("insets -3 5 -1 5") {
    components.foreach(contents+=)
    contents += removeButton
  }

  def extend(ext: JPanel) = {
    if (extendedPanel.isDefined) contents -= extendedPanel.get
    contents += ext
    extendedPanel = Some(ext)
    repaint
    revalidate
  }
}
