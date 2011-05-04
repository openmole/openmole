/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Dimension
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
  
  def setConditionLabel(cond: Option[String])= {
    conditionLabel.setLabel(cond.getOrElse(""))
    setLabelVisible
  }
  
  def setLabelVisible= conditionLabel.setVisible(!conditionLabel.getLabel.isEmpty)
}
        
//
//extends ConnectionWidget {
//
//    LabelWidget conditionLabel;
//
//    public LabeledConnectionWidget(Scene scene, String condition) {
//        super(scene);
//        conditionLabel = new LabelWidget(scene, condition);
//        conditionLabel.setBackground(Constants.getInstance().getColor(Constants.CONDITION_LABEL_BACKGROUND_COLOR));
//        conditionLabel.setBorder(BorderFactory.createLineBorder(Constants.getInstance().getColor(Constants.CONDITION_LABEL_BORDER_COLOR), 2));
//        conditionLabel.setOpaque(true);
//        addChild(conditionLabel);
//        setConstraint(conditionLabel, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f);
//        setMinimumSize(new Dimension(10, 25));
//        setLabelVisible();
//    }
//
//    public void setConditionLabel(String cond) {
//        conditionLabel.setLabel(cond);
//        setLabelVisible();
//    }
//
//    private void setLabelVisible() {
//        conditionLabel.setVisible(!conditionLabel.getLabel().isEmpty());
//    }