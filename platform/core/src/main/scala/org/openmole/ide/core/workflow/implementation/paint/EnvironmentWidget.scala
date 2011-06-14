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

import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openide.util.ImageUtilities
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.model.ICapsuleUI
import org.netbeans.api.visual.widget.Widget
import java.awt.Graphics2D
import java.awt.Container
import java.awt.Point

class EnvironmentWidget(scene: MoleScene,val capsule: ICapsuleUI ) extends Widget(scene) {
  
  override def paintWidget= {
    super.paintWidget
    val environment = capsule.dataProxy.get.dataUI.environment
    if (environment.isDefined){
      val g = getGraphics.asInstanceOf[Graphics2D]
      g.drawImage(ImageUtilities.loadImage(environment.get.dataUI.imagePath),0,0,new Container)
    }
  }
  
  def setDetailedView(w: Int)= setPreferredLocation(new Point(w/2-22,Constants.TASK_CONTAINER_HEIGHT+10))
}