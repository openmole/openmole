/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Container
import java.awt.Graphics2D
import java.awt.Point
import org.netbeans.api.visual.widget.Widget
import org.openide.util.ImageUtilities
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.implementation.MoleScene

class SamplingWidget(scene: MoleScene, factory: IFactoryUI) extends Widget(scene) {
  
  override def paintWidget= {
    super.paintWidget
    getGraphics.asInstanceOf[Graphics2D].drawImage(ImageUtilities.loadImage(factory.imagePath),Constants.TASK_CONTAINER_WIDTH/4+1,Constants.TASK_CONTAINER_HEIGHT+10,new Container)
  }
  
  def setDetailedView(w: Int)= setPreferredLocation(new Point(w/2-44, 14 + Constants.TASK_TITLE_HEIGHT))
}
