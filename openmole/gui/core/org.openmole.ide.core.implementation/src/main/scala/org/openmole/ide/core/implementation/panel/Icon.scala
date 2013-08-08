/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import javax.swing.ImageIcon
import javax.imageio.ImageIO
import org.openmole.ide.core.implementation.data.{ ImageView, DataUI }
import scala.swing.Label

trait Icon {
  def icon(dataUI: DataUI with ImageView) = _icon(dataUI, dataUI.imagePath)

  def fatIcon(dataUI: DataUI with ImageView) = _icon(dataUI, dataUI.fatImagePath)

  private def _icon(dataUI: DataUI, path: String) = new Label {
    icon = new ImageIcon(ImageIO.read(dataUI.getClass.getClassLoader.getResource(path)))
  }
}