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

package org.openmole.ide.misc.widget.example

import scala.swing.MainFrame
import scala.swing.SimpleSwingApplication
import java.awt.BorderLayout
import java.awt.Dimension
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiTextField
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import org.openmole.core.implementation.data.Prototype

object MultiRowExample extends SimpleSwingApplication {
  def top = new MainFrame {
    peer.setLayout(new BorderLayout)
    val proto1 = new Prototype[Int]("proto1")
    val proto2 = new Prototype[Double]("proto2")
    val fake1 = new Fake(proto1)
    val fake2 = new Fake(proto2)
    val action = new ContentAction("Action ", fake1) { override def apply = println("view " + fake1.toString) }
    val image = EYE
    peer.add(new MultiComboLinkLabelGroovyTextFieldEditor("My title",
      List((fake1, proto1, action, "12"), (fake2, proto2, action, "45.6d")),
      List((fake1, proto1, action), (fake2, proto2, action)), image).panel.peer, BorderLayout.WEST)
    peer.add(new MultiComboTextField("My title2",
      List((fake1, "un"), (fake2, "deux")),
      List(fake1, fake2)).panel.peer, BorderLayout.EAST)
    size = new Dimension(250, 200)
  }

  class Fake(p: IPrototype[_]) {
    override def toString = p.name
  }
}
