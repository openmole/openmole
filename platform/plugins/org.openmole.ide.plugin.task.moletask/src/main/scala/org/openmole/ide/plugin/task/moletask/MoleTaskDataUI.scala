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
import org.openmole.misc.exception.UserBadDataError

class MoleTaskDataUI(val name: String="",
			     val mole: Option[IMoleScene] = None) extends TaskDataUI {

  def coreObject = mole match {
//    case Some(x: IMoleScene) => new MoleTask(name, MoleMaker.buildMole(x.manager)._1)
    case _=> throw new UserBadDataError("No Mole is set ")
  }

  def coreClass = classOf[MoleTask] 
  
  def imagePath = "img/mole.png" 
  
  override def fatImagePath = "img/mole_fat.png" 
  
  def buildPanelUI = new MoleTaskPanelUI(this)
 
  def borderColor = new Color(0,102,128)
  
  def backgroundColor = new Color(0,102,128,128)
}
