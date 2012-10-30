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
import java.awt.Graphics2D
import scala.swing._

class MyMigPanel(mig1: String, mig2: String = "", mig3: String = "") extends MigPanel(mig1, mig2, mig3) {
  background = new Color(77, 77, 77)

  override def paintComponent(g: Graphics2D) = {
    contents.foreach(c ⇒ c match {
      case x: TextField ⇒ x.foreground = Color.BLACK
      case x: UIElement ⇒ x.foreground = Color.WHITE
      case _ ⇒
    })
  }
}
