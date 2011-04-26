/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import java.awt.Color
import org.openmole.ide.core.workflow.implementation.SamplingUI

trait ISamplingFactoryUI extends IFactoryUI {
  
  override def entity(name: String) = new SamplingUI(name, this)
  
  override def entity = new SamplingUI(this)
  
  // Default border task color
  override def borderColor = new Color(0,255,0)
  
  // Default background task color
  override def backgroundColor = new Color(0,255,0,128)
  
  // Default background task image
  override def imagePath = "img/thumb/default.png"
}
