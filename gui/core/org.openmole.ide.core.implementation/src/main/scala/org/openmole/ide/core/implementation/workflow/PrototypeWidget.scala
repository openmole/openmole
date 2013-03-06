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

  val grey = new Color(180, 180, 180)

  val red = new Color(212, 0, 0)

  def buildEmptySource(scene: IMoleScene, capsule: ICapsuleUI) = buildPrototype(scene, x ⇒ capsule.inputs.size.toString, new Action("") { def apply = println("empty s") }, new Point(19, TASK_CONTAINER_HEIGHT / 2), grey)

  def buildEmptyHook(scene: IMoleScene, capsule: ICapsuleUI) = buildPrototype(scene, x ⇒ capsule.inputs.size.toString, new Action("") { def apply = println("empty h") }, new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2), grey)

  def buildInput(scene: IMoleScene, capsule: ICapsuleUI) = buildPrototype(scene, x ⇒ capsule.inputs.size.toString, new Action("") { def apply = println("input") }, new Point(19, TASK_CONTAINER_HEIGHT / 2), green(scene))

  def buildOutput(scene: IMoleScene, capsule: ICapsuleUI) = buildPrototype(scene, x ⇒ capsule.outputs.size.toString, new Action("") { def apply = println("input") }, new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2), green(scene))

  def buildPrototype(scene: IMoleScene, x: Unit ⇒ String, action: Action, point: Point, initColor: Color) = new PrototypeWidget(scene, x, new LinkLabel(x(), action), point, initColor)

}

import PrototypeWidget._
class PrototypeWidget(scene: IMoleScene,
                      f: Unit ⇒ String,
                      link: LinkLabel,
                      location: Point = new Point(0, 0),
                      initColor: Color,
                      var hooked: Boolean = false) extends ComponentWidget(scene.graphScene, link.peer) {

  link.foreground = Color.WHITE
  var validationColor = initColor
  val dim = 30
  val pos = link.size.width / 2 + 1
  setPreferredBounds(new Rectangle(dim, dim))
  setPreferredLocation(location)
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
