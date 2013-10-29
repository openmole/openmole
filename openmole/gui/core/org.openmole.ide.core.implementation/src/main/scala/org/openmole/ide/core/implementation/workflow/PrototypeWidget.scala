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
import org.openmole.ide.core.implementation.commons.Constants
import Constants._
import org.openmole.ide.misc.widget.LinkLabel
import scala.swing.Action
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI

object PrototypeWidget {
  def green(scene: MoleScene) = scene match {
    case y: BuildMoleScene ⇒ new Color(180, 200, 7, 220)
    case _                 ⇒ new Color(44, 137, 160, 64)
  }

  val grey = new Color(180, 180, 180)

  val red = new Color(212, 0, 0)

  def buildNoTaskSource(scene: MoleScene, capsule: CapsuleUI, color: Color = grey) =
    new IPrototypeWidget(scene, capsule, new LinkLabel("0", new Action("") { def apply = scene.displayCapsuleProperty(capsule, 0) }), new Point(19, TASK_CONTAINER_HEIGHT / 2), grey)

  def buildNoTaskHook(scene: MoleScene, capsule: CapsuleUI, color: Color = grey) =
    new OPrototypeWidget(scene, capsule, new LinkLabel("0", new Action("") { def apply = scene.displayCapsuleProperty(capsule, 1) }), new Point(TASK_CONTAINER_WIDTH - 30, TASK_CONTAINER_HEIGHT / 2), grey)

  def buildTaskSource(scene: MoleScene, capsule: CapsuleUI) = buildNoTaskSource(scene, capsule, green(scene))

  def buildTaskHook(scene: MoleScene, capsule: CapsuleUI) = buildNoTaskHook(scene, capsule, green(scene))

  class IPrototypeWidget(scene: MoleScene,
                         capsule: CapsuleUI,
                         link: LinkLabel,
                         location: Point = new Point(0, 0),
                         initColor: Color) extends PrototypeWidget(scene, capsule, link, location, initColor) {
    def activated = !capsule.dataUI.sources.isEmpty
    override def paintChildren = link.text = List(capsule.dataUI.task).flatten.foldLeft(0)((acc, t) ⇒ acc + t.dataUI.inputs.size).toString
  }

  class OPrototypeWidget(scene: MoleScene,
                         capsule: CapsuleUI,
                         link: LinkLabel,
                         location: Point = new Point(0, 0),
                         initColor: Color) extends PrototypeWidget(scene, capsule, link, location, initColor) {
    def activated = !capsule.dataUI.hooks.isEmpty
    override def paintChildren = link.text = List(capsule.dataUI.task).flatten.foldLeft(0)((acc, t) ⇒ acc + t.dataUI.outputs.size).toString
  }
}

import PrototypeWidget._

abstract class PrototypeWidget(scene: MoleScene,
                               capsule: CapsuleUI,
                               link: LinkLabel,
                               location: Point = new Point(0, 0),
                               initColor: Color) extends ComponentWidget(scene.graphScene, link.peer) {

  def activated: Boolean

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

  override def paintBackground = {
    val g = scene.graphScene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(if (!scene.dataUI.capsulesInMole.toList.contains(capsule)) new Color(150, 150, 150) else validationColor)
    g.fillOval(pos, pos, dim, dim)
    g.setStroke(new BasicStroke(3f))

    g.setColor(if (activated) new Color(73, 90, 105)
    else new Color(77, 77, 77, 150))

    val offset = dim - 2 * { if (activated) 1 else 0 }
    g.drawOval(pos, pos, offset, offset)

    if (activated) {
      g.setColor(new Color(73, 90, 105))
      g.setStroke(new BasicStroke(3f))
      g.drawOval(pos - 2, pos - 2, offset + 4, offset + 4)
    }

    revalidate
  }

}
