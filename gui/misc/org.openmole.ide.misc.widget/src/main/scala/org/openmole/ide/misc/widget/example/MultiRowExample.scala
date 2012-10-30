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

import scala.swing.ComboBox
import scala.swing.Component
import scala.swing.MainFrame
import scala.swing.SimpleSwingApplication
import java.awt.BorderLayout
import java.awt.Dimension
import org.openmole.ide.misc.widget.multirow.RowWidget
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiPanel
import org.openmole.ide.misc.widget.multirow.MultiTextField
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.core.model.data._
import org.openmole.ide.misc.tools.image.Images._
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.widget.multirow._
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import org.openmole.core.model.data._
import scala.swing.TextField

object MultiRowExample extends SimpleSwingApplication {
  def top = new MainFrame {
    peer.setLayout(new BorderLayout)
    val proto1 = Prototype[Int]("proto1")
    val proto2 = Prototype[Double]("proto2")
    val fake1 = new Fake(proto1)
    val fake2 = new Fake(proto2)
    val action = new ContentAction("Action ", fake1) { override def apply = println("view " + fake1.toString) }
    val image = EYE
    val customPanel = new CustomPanel(new Data("yo", "77", 2))
    peer.add(new MultiPanel("Panels",
      Factory,
      List(customPanel)).panel.peer, BorderLayout.CENTER)
    size = new Dimension(250, 200)
  }

  class Fake(p: Prototype[_]) {
    override def toString = p.name
  }

  object Factory extends IFactory[Data] {
    def apply = new CustomPanel(new Data)
  }

  class Data(val name: String = "",
             val id: String = "",
             val nb: Int = 0) extends IData

  class CustomPanel(data: Data) extends PluginPanel("wrap") with IPanel[Data] {
    val t1 = new TextField(data.name)
    val t2 = new TextField(data.id)
    val c1 = new ComboBox[Int](List(1, 2, 3)) { selection.item = data.nb }

    contents += t1
    contents += t2
    contents += c1

    def content = new Data(t1.text, t2.text, c1.selection.item)
  }
}
