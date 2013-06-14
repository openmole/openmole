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

package org.openmole.ide.misc.widget.multirow

import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.Component
import scala.swing.event.FocusGained

object MultiPanel {
  class Factory[D <: IData](panelFactory: IFactory[D],
                            plus: Plus) extends IRowWidgetFactory[D, PanelRowWidget[D]] {
    def apply = new PanelRowWidget(panelFactory.apply, plus)
  }

  class PanelRowWidget[D <: IData](val p: IPanel[D], plus: Plus) extends IRowWidget[D] {

    override val panel = new RowPanel(List(p), plus)

    override def content = p.content

    def contents = p.contents
  }
}

import MultiPanel._
class MultiPanel[D <: IData, P <: IPanel[D]](title: String,
                                             panelFactory: IFactory[D],
                                             initPanels: Seq[P],
                                             minus: Minus = NO_EMPTY,
                                             plus: Plus = ADD) extends MultiWidget(title,
  initPanels.map { p ⇒ new PanelRowWidget(p, plus) },
  new Factory(panelFactory, plus),
  minus,
  false) with IMultiPanel[D] {
  def content = rowWidgets.map(_.content).toList
}

