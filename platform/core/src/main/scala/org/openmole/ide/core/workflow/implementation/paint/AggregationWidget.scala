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
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.openide.util.ImageUtilities
import org.openmole.ide.core.commons.TransitionType._
import org.openmole.ide.core.workflow.implementation.TransitionUI

//class AggregationWidget (val scene: Scene, transition: TransitionUI) extends Widget(scene) {
class AggregationWidget (val scene: Scene, transition: TransitionUI) extends LabelWidget(scene,"AA") {

  override def paintWidget= {
    super.paintWidget
    if (transition.transitionType == AGGREGATION_TRANSITION){
      val g = getGraphics.asInstanceOf[Graphics2D]
      g.drawImage(ImageUtilities.loadImage("/home/mathieu/Bureau/cubes.png"),0,0,new Container)
      println("AGGREG")
    }
  }
}
