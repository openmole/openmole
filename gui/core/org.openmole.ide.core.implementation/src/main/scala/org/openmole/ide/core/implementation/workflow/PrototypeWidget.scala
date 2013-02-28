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

package org.openmole.ide.core.implementation.workflow

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import org.netbeans.api.visual.widget.ComponentWidget
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.{ ICapsuleUI, IMoleScene }
import org.openmole.ide.misc.widget.LinkLabel
import scala.swing.Action
import scala.swing.Label

object PrototypeWidget {
  def green(scene: IMoleScene) = scene match {
    case y: BuildMoleScene ⇒ new Color(180, 200, 7, 220)
    case _ ⇒ new Color(44, 137, 160, 64)
  }

  val red = new Color(212, 0, 0)

  def buildEmptySource(scene: IMoleScene) = {
    new PrototypeWidget(scene, x ⇒ "0",
      new LinkLabel("0", new Action("") {
        def apply =
          println("nada")
      })) {
      setPreferredLocation(new Point(19, TASK_CONTAINER_HEIGHT / 2))
    }
  }

  def buildEmptyHook(scene: IMoleScene) = {
    new PrototypeWidget(scene, x ⇒ "0",
      new LinkLabel("0", new Action("") {
        def apply =
          println("nada")
      })) {
      setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2))
    }
  }

  def buildInput(scene: IMoleScene, capsule: ICapsuleUI) = {

    new PrototypeWidget(scene, x ⇒ capsule.inputs.size.toString,
      new LinkLabel(capsule.inputs.size.toString, new Action("") {
        def apply = println("display capsule execution panel center on sources")
      })) {
      setPreferredLocation(new Point(19, TASK_CONTAINER_HEIGHT / 2))
    }
  }

  def buildOutput(scene: IMoleScene, capsule: ICapsuleUI) = {
    new PrototypeWidget(scene, x ⇒ capsule.outputs.size.toString,
      new LinkLabel(capsule.outputs.size.toString, new Action("") {
        def apply = println("display capsule execution panel center on hooks")
      })) {
      setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2))
    }
  }

}

import PrototypeWidget._
class PrototypeWidget(scene: IMoleScene,
                      f: Unit ⇒ String,
                      link: Label,
                      var hooked: Boolean = false) extends ComponentWidget(scene.graphScene, link.peer) {
  link.foreground = Color.WHITE
  var validationColor = green(scene)
  val dim = 30
  val pos = link.size.width / 2 + 1
  setPreferredBounds(new Rectangle(dim, dim))
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  setOpaque(true)

  def updateErrors(errorString: String) = {
    validationColor = errorString.isEmpty match {
      case true ⇒ green(scene)
      case false ⇒
        link.tooltip = errorString
        red
    }
    revalidate
  }

  override def paintChildren = link.text = f()

  override def paintBackground = {
    val g = scene.graphScene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(validationColor)
    g.fillOval(pos, pos, dim, dim)
    revalidate
  }

  override def paintBorder = {
    val g = scene.graphScene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.setStroke(new BasicStroke(3f))
    g.setColor(new Color(77, 77, 77, 150))

    val offset1 = dim - 2 * { if (hooked) 1 else 0 }
    g.drawOval(pos, pos, offset1, offset1)

    if (hooked) {
      g.setColor(new Color(230, 180, 25))
      g.setStroke(new BasicStroke(5f))
      g.drawOval(pos - 4, pos - 4, offset1 + 8, offset1 + 8)
    }

    revalidate
  }
}
