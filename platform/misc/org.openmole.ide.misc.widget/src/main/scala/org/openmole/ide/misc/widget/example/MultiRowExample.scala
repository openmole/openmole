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
import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.ImageIcon
import javax.swing.UIManager
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTextField
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.netbeans.api.visual.border.BorderFactory
import org.openide.util.ImageUtilities
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import scala.swing.TextField
import org.openmole.core.implementation.data.Prototype

object MultiRowExample extends SimpleSwingApplication {
  def top = new MainFrame {
    peer.setLayout(new BorderLayout)
    val fake1 = new Fake("fake1")
    val fake2 = new Fake("fake2")
    val proto1 = new Prototype("proto1",classOf[Int])
    val proto2 = new Prototype("proto1",classOf[Double])
    val action = new ContentAction("Action " , fake1) { override def apply = println("view " + fake1.toString)}
    val image = new ImageIcon(ImageUtilities.loadImage("img/eye.png"))
    peer.add(new MultiComboLinkLabelGroovyTextFieldEditor("My title",
      List((fake1,proto1.`type`,action,"one"),(fake2,proto2.`type`,action,"")),
      List((fake1,proto1.`type`,action),(fake2,proto2.`type`,action)),image).panel.peer,BorderLayout.WEST)
    peer.add(new MultiTextField("My title2",
      List("un","deux")).panel.peer,BorderLayout.EAST)
    size = new Dimension(250,200)
  }
  
  class Fake(s: String) {
    override def toString = s
  }
}
