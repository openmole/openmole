/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.ui.ide.workflow.implementation.PropertyManager
import org.openide.util.ImageUtilities
import java.awt.Color
import java.awt.Image
import java.util.Properties

trait IObjectViewUI {
  def properties: Properties
  
  override def backgroundColor= getColor(PropertyManager.BG_COLOR)
  
  override def borderColor= getColor(PropertyManager.BORDER_COLOR)
               
  override def backgroundImage: Image= ImageUtilities.loadImage(properties.getProperty(PropertyManager.BG_IMG))
  
  def getColor(colorString: String)= {
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