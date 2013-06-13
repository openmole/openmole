/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.misc.widget.multirow

import java.awt.Color
import scala.collection.mutable.HashSet
import scala.swing.Action
import scala.swing.Component
import scala.swing.Label
import org.openmole.ide.misc.tools.image.Images.ADD
import org.openmole.ide.misc.widget.ImageLinkLabel
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.event.FocusGained

object MultiWidget extends Enumeration {

  class Minus(val name: String) extends Val(name)

  val NO_EMPTY = new Minus("NO_EMPTY")
  val CLOSE_IF_EMPTY = new Minus("CLOSE_IF_EMPTY")
}

import MultiWidget._
class MultiWidget[S, T <: IRowWidget[S]](title: String = "",
                                         rWidgets: Seq[T],
                                         factory: IRowWidgetFactory[S, T],
                                         allowEmpty: MultiWidget.Minus = MultiWidget.NO_EMPTY,
                                         buildRowFromFactory: Boolean = false) extends Component {
  val rowWidgets = new HashSet[T]
  val panel = new PluginPanel("wrap " + {
    rWidgets.headOption match {
      case Some(x: IRowWidget[T]) ⇒ if (x.plusAllowed == ADD) 1 else 0
      case _                      ⇒ 0
    }
  }.toString + ", insets 0 5 0 5")
  val titleLabel = new Label(title) { foreground = new Color(0, 113, 187) }
  val addButton = new ImageLinkLabel(ADD, new Action("") { def apply = addRow })

  rWidgets.foreach(addRow)

  if (!title.isEmpty) panel.contents.insert(0, titleLabel)
  panel.contents += addButton

  def addRow: T = addRow(factory.apply)

  def addRow(rowWidget: T): T = {
    rowWidgets += rowWidget
    panel.contents.insert(panel.contents.size - 1, rowWidget.panel)

    rowWidget.panel.removeButton.action = new Action("") {
      def apply = {
        if (allowEmpty == CLOSE_IF_EMPTY || (allowEmpty == NO_EMPTY && rowWidgets.size > 1)) {
          removeRow(rowWidget)
          rowWidget.doOnClose
        }
      }
    }

    listenTo(rowWidget.contents.toSeq: _*)
    reactions += {
      case FocusGained(source: Component,
        other: Option[Component],
        temporary: Boolean) ⇒
        publish(new ComponentFocusedEvent(this))
      case _ ⇒
    }

    refresh
    rowWidget
  }

  def removeAllRows = rowWidgets.foreach(removeRow)

  def removeRow(rowWidget: T) = {
    rowWidgets -= rowWidget
    panel.contents -= rowWidget.panel
    refresh
  }

  def refresh = {
    panel.repaint
    panel.revalidate
  }
}
