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
  
  def backgroundColor: Color
  
  def borderColor: Color
               
  def backgroundImage: Image
}

//interface IObjectViewUI{
//    IObjectModelUI getModel();
//    Color getBackgroundColor();
//    Color getBorderColor();
//    Image getBackgroundImage();
//    MyWidget getWidget();
//    IMoleScene getMoleScene();