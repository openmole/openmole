/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow

import java.awt.Dimension
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.border.BorderFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.openmole.ide.core.commons.Constants
import org.netbeans.api.visual.layout.LayoutFactory
import org.openmole.ide.core.commons.TransitionType._

class LabeledConnectionWidget(val scene: Scene,val transition: TransitionUI) extends ConnectionWidget(scene) {
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
  
