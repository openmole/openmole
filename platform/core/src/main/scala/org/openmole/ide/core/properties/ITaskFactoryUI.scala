/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import java.awt.Color
import org.openmole.ide.core.workflow.implementation.TaskUI

trait ITaskFactoryUI extends IFactoryUI{

//  override def buildEntity(name: String,panel: PanelUI) = new TaskUI(name,coreClass,borderColor,backgroundColor, panel)
//  
//  override def buildEntity(panel: PanelUI) = new TaskUI(coreClass,borderColor,backgroundColor,panel)
  
  def borderColor: Color
  
  def backgroundColor: Color
}
