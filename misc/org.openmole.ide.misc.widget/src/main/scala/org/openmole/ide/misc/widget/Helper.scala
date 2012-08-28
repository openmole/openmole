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

package org.openmole.ide.misc.widget

import java.awt.Color
import java.awt.Font
import java.awt.Font._
import javax.swing.BorderFactory
import scala.collection.mutable.HashMap
import scala.swing.Component
import scala.swing.TextArea
import scala.swing.Label

class Helper extends PluginPanel("wrap") {
  val _helpMap = new HashMap[Component, Help]

  def add(component: Component,
          help: Help) = _helpMap += component -> help

  def components = _helpMap.keys

  def switchTo(component: Component) = {
    contents.removeAll
    contents += new PluginPanel("wrap") {
      _helpMap.contains(component) match {
        case true ⇒
          val help = _helpMap(component)
          contents += new HelpTextArea(help.message)
          contents += new ExampleHelpTextArea(help.example)
          help.urls.foreach { u ⇒ contents += new ExternalLinkLabel(u.text, u.url)
          }
        case false ⇒ new Label { opaque = false }
      }
    }
    revalidate
    repaint
  }

  class HelpTextArea(t: String) extends TextArea(t, 1, 40) {
    border = BorderFactory.createEmptyBorder
    background = new Color(77, 77, 77)
    lineWrap = true
    wordWrap = true
    editable = false
  }

  class ExampleHelpTextArea(t: String) extends HelpTextArea(t) {
    font = new Font(font.getFamily, ITALIC, font.getSize)
  }
}
