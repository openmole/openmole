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
package org.openmole.ide.core.implementation.workflow

import java.awt.Dimension
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.border.BorderFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.openmole.ide.core.model.commons.Constants
import org.netbeans.api.visual.layout.LayoutFactory
import org.openmole.ide.core.model.workflow.ITransitionUI
import org.openmole.ide.core.model.commons.TransitionType._

class LabeledConnectionWidget(val scene: Scene,val transition: ITransitionUI) extends ConnectionWidget(scene){
  val conditionLabel = new LabelWidget(scene, transition.condition.getOrElse(""))
  conditionLabel.setBackground(Constants.CONDITION_LABEL_BACKGROUND_COLOR)
  conditionLabel.setBorder(BorderFactory.createLineBorder(2,Constants.CONDITION_LABEL_BORDER_COLOR))
  conditionLabel.setOpaque(true)
  addChild(conditionLabel)
  setConstraint(conditionLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  setMinimumSize(new Dimension(10, 25))
  setLabelVisible
  drawTransitionType
  
  def setConditionLabel(cond: Option[String])= {
    conditionLabel.setLabel(cond.getOrElse(""))
    setLabelVisible
  }
  
  def setLabelVisible= conditionLabel.setVisible(!conditionLabel.getLabel.isEmpty)
  
  def drawTransitionType = {
    transition.transitionType match {
      case EXPLORATION_TRANSITION=> setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.EXPLORATION_TRANSITON,false))
      case AGGREGATION_TRANSITION=> setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.AGGREGATION_TRANSITON,false))
      case _=> setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
    }
  }
  
}
  
