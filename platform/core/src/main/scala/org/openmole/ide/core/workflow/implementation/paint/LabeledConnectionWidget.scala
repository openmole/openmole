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
import org.netbeans.api.visual.widget.ImageWidget
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.openide.util.ImageUtilities
import org.openmole.ide.core.commons.Constants
import org.netbeans.api.visual.layout.LayoutFactory
import org.openmole.ide.core.workflow.implementation.TransitionUI

class LabeledConnectionWidget(val scene: Scene, transition: TransitionUI) extends ConnectionWidget(scene) {
  val conditionLabel = new LabelWidget(scene, transition.condition.getOrElse(""))
  conditionLabel.setBackground(Constants.CONDITION_LABEL_BACKGROUND_COLOR)
  conditionLabel.setBorder(BorderFactory.createLineBorder(2,Constants.CONDITION_LABEL_BORDER_COLOR))
  conditionLabel.setOpaque(true)
  addChild(conditionLabel)
  setConstraint(conditionLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  setMinimumSize(new Dimension(10, 25))
  setLabelVisible
  
//  val aggregWidget = new AggregationWidget(scene,transition)
//  aggregWidget.setOpaque(true)
//  addChild(aggregWidget)
//  setConstraint(aggregWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
//  setMinimumSize(new Dimension(50, 50))
  
  val aggregLabel = new ImageWidget(scene,ImageUtilities.loadImage("/home/mathieu/Bureau/cubes.png"))
 // aggregLabel.setBackground(new Color(222,0,123))
 // aggregLabel.setOpaque(true)
  addChild(aggregLabel)
  setConstraint(aggregLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.TOP_CENTER, 0.5f)
 // setMinimumSize(new Dimension(50, 50))
 setAsAggregationTransition(true)
  
  def setConditionLabel(cond: Option[String])= {
    conditionLabel.setLabel(cond.getOrElse(""))
    setLabelVisible
  }
  
  def setLabelVisible= conditionLabel.setVisible(!conditionLabel.getLabel.isEmpty)
  
  def setAsAggregationTransition(b: Boolean) = {
    aggregLabel.setVisible(b)
    println("set Visible " + b)
//    scene.repaint
  //  scene.revalidate
  }
//  
//  override def paintWidget = {
//    super.paintWidget
//    val graphics = getGraphics.asInstanceOf[Graphics2D]
//    if (transition.isAggregation){
//      graphics.setColor(new Color(255,0,0))
//      graphics.fill(new Rectangle(0,0,40,40))
//    }
//  }
}
  