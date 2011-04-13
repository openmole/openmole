/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.workflow.implementation.PropertyManager
import org.openide.util.ImageUtilities
import java.awt.Color
import java.awt.Image
import java.util.Properties

trait IObjectViewUI {
  def properties: Properties
  
  var backgroundColor= color(PropertyManager.BG_COLOR)
  
  var borderColor= color(PropertyManager.BORDER_COLOR)
               
  var backgroundImage= Some(ImageUtilities.loadImage(properties.getProperty(PropertyManager.BG_IMG)))
  
  def color(colorString: String)= {
    val colors= properties.getProperty(colorString).split(",")
    new Color(colors(0).toInt,
              colors(1).toInt,
              colors(2).toInt)
  }  
}

//interface IObjectViewUI{
//    IObjectModelUI getModel();
//    Color getBackgroundColor();
//    Color getBorderColor();
//    Image getBackgroundImage();
//    MyWidget getWidget();
//    IMoleScene getMoleScene();