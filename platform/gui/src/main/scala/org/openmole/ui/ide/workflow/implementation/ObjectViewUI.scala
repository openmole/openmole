/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.ide.workflow.implementation

import java.awt.Color
import org.netbeans.api.visual.widget.Widget
import org.openide.util.ImageUtilities
import org.openmole.ui.ide.workflow.model.IObjectViewUI

class ObjectViewUI(scene: MoleScene) extends Widget(scene) with IObjectViewUI{
  var backgroundColor= color(PropertyManager.BG_COLOR)
  
  var borderColor= color(PropertyManager.BORDER_COLOR)
               
  var backgroundImage= ImageUtilities.loadImage(properties.getProperty(PropertyManager.BG_IMG))
  
  def color(colorString: String)= {
    val colors= properties.getProperty(colorString).split(",")
    new Color(colors(0).toInt,
              colors(1).toInt,
              colors(2).toInt)
  }
}