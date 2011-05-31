/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import org.netbeans.api.visual.border.BorderFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.openmole.ide.core.commons.Constants
import org.netbeans.api.visual.layout.LayoutFactory

class LabeledConnectionWidget(val scene: Scene, condition: Option[String]) extends ConnectionWidget(scene) {
  val conditionLabel = new LabelWidget(scene, condition.getOrElse(""))
  conditionLabel.setBackground(Constants.CONDITION_LABEL_BACKGROUND_COLOR)
  conditionLabel.setBorder(BorderFactory.createLineBorder(2,Constants.CONDITION_LABEL_BORDER_COLOR))
  conditionLabel.setOpaque(true)
  addChild(conditionLabel)
  setConstraint(conditionLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  setMinimumSize(new Dimension(10, 25))
  setLabelVisible
  
  val aggregLabel = new LabelWidget(scene)
  aggregLabel.setBackground(Constants.CONDITION_LABEL_BACKGROUND_COLOR)
  aggregLabel.setOpaque(true)
  addChild(aggregLabel)
  setConstraint(aggregLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER_TARGET, 0.5f)
  setMinimumSize(new Dimension(10, 25))
  aggregLabel.setVisible(true)
  
  def setConditionLabel(cond: Option[String])= {
    conditionLabel.setLabel(cond.getOrElse(""))
    setLabelVisible
  }
  
  def setLabelVisible= conditionLabel.setVisible(!conditionLabel.getLabel.isEmpty)
  
  def setAsAggregationTransition(b: Boolean) = {
    aggregLabel.setVisible(b)
    println("set Visible " + b)
  }
}
  