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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

class PropertyPanel(borderColor: Color,
                    mig1: String, 
                    mig2: String ="", 
                    mig3: String="") extends MyMigPanel(mig1,mig2,mig3) {

  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    g.setColor(new Color(77,77,77))
    g.fillRoundRect(0, 0, size.width, size.height,20, 20)
    g.setStroke(new BasicStroke(5f))
    g.setColor(borderColor)
    g.drawRoundRect(1,1,size.width -4,size.height-4,20,20)
  }  
}
