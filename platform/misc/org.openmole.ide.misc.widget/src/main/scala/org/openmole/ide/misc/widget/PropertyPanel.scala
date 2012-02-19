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
import java.awt.RenderingHints

class PropertyPanel(mig1: String, mig2: String ="", mig3: String="") extends MigPanel(mig1) {

  override def paintComponent(g: Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(new Color(0, 0, 0, 200))
    g.fillRoundRect(0, 0, size.width, size.height,10, 10)
    revalidate
  }  
}
