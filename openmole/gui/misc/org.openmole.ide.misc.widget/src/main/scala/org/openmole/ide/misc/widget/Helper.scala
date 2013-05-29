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

class Helper(val permalinks: List[URL] = List.empty) extends PluginPanel("wrap") {
  val helpMap = new HashMap[Component, Help]

  addPermalinks

  def add(component: Component,
          help: Help) = helpMap += component -> help

  def components = helpMap.keys

  def addPermalinks = permalinks foreach {
    addExternalLink(_, true)
  }

  def switchTo(help: Help) = {
    contents += new HelpTextArea(help.message)
    contents += new ExampleHelpTextArea(help.example)
    addPermalinks
    help.urls foreach {
      addExternalLink(_, false)
    }
  }

  def switchTo(component: Component): Unit = {
    contents.removeAll
    contents += new PluginPanel("wrap") {
      helpMap.contains(component) match {
        case true ⇒ switchTo(helpMap(component))
        case false ⇒ permalinks foreach {
          addExternalLink(_, true)
        }
      }
    }
    revalidate
    repaint
  }

  private def addExternalLink(u: URL, b: Boolean) = contents += new ExternalLinkLabel(u.text, u.url, bold = b)

  class HelpTextArea(t: String) extends TextArea(t, 1, 40) {
    border = BorderFactory.createEmptyBorder
    background = new Color(77, 77, 77)
    lineWrap = true
    wordWrap = true
    editable = false
  }

  class ExampleHelpTextArea(t: String) extends HelpTextArea({
    if (!t.isEmpty) "Ex: " else ""
  } + t) {
    font = new Font(font.getFamily, ITALIC, font.getSize)
  }

}
