/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.moletask

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.core.implementation.task.MoleTask
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.workflow.IMoleScene

class MoleTaskDataUI(val name: String="",
			     val mole: IMoleScene= null) extends TaskDataUI {

  def coreObject = new MoleTask(name, MoleMaker.buildMole(mole.manager)._1)

  def coreClass = classOf[MoleTask] 
  
  def imagePath = "img/mole.png" 
  
  override def fatImagePath = "img/mole_fat.png" 
  
  def buildPanelUI = new MoleTaskPanelUI(this)
 
  def borderColor = new Color(61,104,130)
  
  def backgroundColor = new Color(61,104,130,128)
}
