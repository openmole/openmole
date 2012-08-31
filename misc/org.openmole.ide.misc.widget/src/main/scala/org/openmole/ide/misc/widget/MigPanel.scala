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

package org.openmole.ide.misc.widget

import swing._
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout
import scala.collection.mutable._

class MigPanel(constraints: String, colConst: String, rowConst: String) extends MyPanel with LayoutContainer {

  def this(constraints: String) = this(constraints, "", "")

  type Constraints = String

  def layoutManager = peer.getLayout.asInstanceOf[MigLayout]

  override lazy val peer = new JPanel(new MigLayout(constraints, colConst, rowConst)) with SuperMixin

  override def contents: MigContent = new MigContent
  protected class MigContent extends Content {
    def +=(c: Component, l: Constraints) = add(c, l)
    def +=(p: JPanel) = peer.add(p)
    def -=(p: JPanel) = peer.remove(p)
    def removeAll = peer.removeAll
  }

  protected def constraintsFor(comp: Component) =
    layoutManager.getConstraintMap.get(comp.peer).toString

  protected def areValid(c: Constraints): (Boolean, String) = (true, "")

  protected def add(c: Component, l: Constraints) = peer.add(c.peer, l)
}
