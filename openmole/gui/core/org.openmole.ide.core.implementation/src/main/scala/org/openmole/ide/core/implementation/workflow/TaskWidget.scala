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

import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import javax.imageio.ImageIO
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow._
import java.awt._
import scala.swing.Panel
import swing.event.MouseClicked

class TaskWidget(scene: IMoleScene,
                 val capsule: ICapsuleUI) extends Panel {
  peer.setLayout(new BorderLayout)
  preferredSize = new Dimension(TASK_CONTAINER_WIDTH, TASK_CONTAINER_HEIGHT)
  override def paint(g: Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)

    g.setPaint(backColor)
    g.fillRoundRect(0, 0, preferredSize.width, preferredSize.height, 5, 5)
    g.setColor(borderColor)
    g.setStroke(new BasicStroke(5))
    g.draw(new Rectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1))
    g.fillRect(0, 0, preferredSize.width, TASK_TITLE_HEIGHT)

    capsule.dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        g.drawImage(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.fatImagePath)), 10, 30, 80, 80, peer)
      case None ⇒
    }
    // g.setFont(new Font("Ubuntu", Font.PLAIN, 32))
    // if (capsule.dataUI.capsuleType == CapsuleType.STRAINER_CAPSULE)
    //   g.drawImage(ImageIO.read(capsule.dataUI.getClass.getClassLoader.getResource("img/" + CapsuleType.toString(capsule.dataUI.capsuleType).toLowerCase + "Capsule.png")), -5, -5, 20, 20, peer)
  }

  def backColor = {
    capsule.dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        scene match {
          case y: BuildMoleScene ⇒
            new Color(215, 238, 244)
          case _ ⇒
            new Color(215, 238, 244, 64)
        }
      case _ ⇒
        new Color(215, 238, 244)
    }
  }

  def borderColor: Color = {
    if (capsule.selected) new Color(222, 135, 135)
    else {
      capsule.dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒
          scene match {
            case y: BuildMoleScene ⇒ new Color(73, 90, 105)
            case _                 ⇒ new Color(44, 137, 160, 64)
          }
        case _ ⇒ new Color(73, 90, 105)
      }
    }
  }
}
