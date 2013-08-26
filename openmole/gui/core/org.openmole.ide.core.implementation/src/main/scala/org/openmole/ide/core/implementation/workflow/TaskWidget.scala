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

import javax.imageio.ImageIO
import org.openmole.ide.core.implementation.commons.Constants
import Constants._
import java.awt._
import scala.swing.{ Action, Panel }
import org.openmole.ide.misc.widget.ImageLinkLabel
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI

object TaskWidget {
  lazy val VALID = (new Color(215, 238, 244), new Color(73, 90, 105))
  lazy val INVALID = (new Color(225, 160, 170), new Color(212, 0, 0))
  lazy val EXECUTION = (new Color(215, 238, 244, 64), new Color(44, 137, 160, 64))
  //lazy val NOT_RUNNABLE = (new Color(215, 238, 244),new Color(73, 90, 105))
  lazy val NOT_RUNNABLE = (new Color(150, 150, 150, 100), new Color(80, 80, 80, 100))

}

import TaskWidget._

class TaskWidget(scene: MoleScene,
                 val capsule: CapsuleUI) extends Panel {
  preferredSize = new Dimension(TASK_CONTAINER_WIDTH, TASK_CONTAINER_HEIGHT)
  background = new Color(0, 0, 0, 0)

  val settings = new ImageLinkLabel(Images.SETTINGS, new Action("") {
    def apply = {
      capsule.dataUI.task match {
        case Some(x: TaskDataProxyUI) ⇒ scene.saveAndcloseAllAndDisplayPropertyPanel(x)
        case _                        ⇒
      }
    }
  })

  peer.setLayout(null)
  settings.peer.setBounds(5, 3, 15, 15)
  peer.add(settings.peer)

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
      case Some(x: TaskDataProxyUI) ⇒
        g.drawImage(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.fatImagePath)), 10, 30, 80, 80, peer)
      case None ⇒
    }
    super.paint(g)
    // g.setFont(new Font("Ubuntu", Font.PLAIN, 32))
    // if (capsule.dataUI.capsuleType == CapsuleType.STRAINER_CAPSULE)
    //   g.drawImage(ImageIO.read(capsule.dataUI.getClass.getClassLoader.getResource("img/" + CapsuleType.toString(capsule.dataUI.capsuleType).toLowerCase + "Capsule.png")), -5, -5, 20, 20, peer)
  }

  def backColor = {
    if (scene.dataUI.capsulesInMole.toList.contains(capsule)) {
      capsule.dataUI.task match {
        case Some(x: TaskDataProxyUI) ⇒
          scene match {
            case y: BuildMoleScene ⇒
              if (capsule.valid) VALID._1 else INVALID._1
            case _ ⇒ EXECUTION._1

          }
        case _ ⇒ NOT_RUNNABLE._1
      }
    }
    else NOT_RUNNABLE._1
  }

  def borderColor: Color = {
    if (scene.dataUI.capsulesInMole.toList.contains(capsule)) {
      if (capsule.selected) new Color(222, 135, 135)
      else {
        capsule.dataUI.task match {
          case Some(x: TaskDataProxyUI) ⇒
            scene match {
              case y: BuildMoleScene ⇒ if (capsule.valid) VALID._2 else INVALID._2
              case _                 ⇒ EXECUTION._2
            }
          case _ ⇒ NOT_RUNNABLE._2
        }
      }
    }
    else NOT_RUNNABLE._2
  }
}
