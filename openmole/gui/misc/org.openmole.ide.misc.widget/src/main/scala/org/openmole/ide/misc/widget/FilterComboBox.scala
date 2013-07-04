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
import event.{ SelectionChanged, MousePressed }
import javax.swing.{ JTextField, ImageIcon }
import javax.imageio.ImageIO
import java.io.File
import java.awt.Color
import swing.BorderPanel.Position._
import java.awt.event.KeyAdapter
import org.openmole.ide.misc.tools.image.Images

object FilterComboBox {
  def apply[T](initialItems: Seq[T]) = new FilterComboBox(initialItems)
}

class FilterComboBox[T](initialItems: Seq[T]) extends PluginPanel("wrap 2, insets 5 5 5 1") {
  fcb ⇒

  implicit def componentToString(c: java.awt.Component): String = c match {
    case tf: JTextField ⇒ tf.getText
    case _              ⇒ ""
  }

  // println("img : " + this.getClass.getClassLoader.getResource("img/filter.png").getFile)
  val label = new Label {
    icon = Images.FILTER
    background = new Color(0, 0, 0, 0)
    border = Swing.EmptyBorder
  }
  val combo = new MyComboBox(initialItems)
  val editor = combo.peer.getEditor.getEditorComponent.asInstanceOf[JTextField]
  editor.addKeyListener(new KeyAdapter {
    override def keyPressed(event: java.awt.event.KeyEvent) {
      if (event.getKeyCode == java.awt.event.KeyEvent.VK_ENTER) {
        setEditorText(combo.selection.item.toString)
      }
    }

    override def keyReleased(event: java.awt.event.KeyEvent) {
      val t = editor.getText
      if (event.getKeyCode != java.awt.event.KeyEvent.VK_SHIFT) {
        val ii = combo.items.filter {
          _.toString.startsWith(editor)
        }
        combo.peer.setModel(MyComboBox.newConstantModel(ii))
        setEditorText(t)
        combo.peer.showPopup
      }
    }
  })
  contents += label
  contents += combo

  reactions += {
    case (e: MousePressed) ⇒ enterFilteringMode
    case SelectionChanged(_) ⇒
      if (combo.peer.isEditable) {
        val selected = combo.selection.item
        clear
        combo.selection.item = selected
      }
  }
  listenTo(combo.selection, label.mouse.clicks)

  def enterFilteringMode = {
    combo.peer.setEditable(true)
    combo.peer.getEditor.getEditorComponent match {
      case tf: JTextField ⇒
        tf.setText("")
        tf.requestFocus
      case _ ⇒
    }
  }

  def clear = {
    setEditorText("")
    combo.peer.setEditable(false)
    combo.peer.setModel(MyComboBox.newConstantModel(initialItems))
  }

  def setEditorText(s: String): Unit = editor match {
    case tf: JTextField ⇒ tf.setText(s)
    case _              ⇒ ""
  }
}