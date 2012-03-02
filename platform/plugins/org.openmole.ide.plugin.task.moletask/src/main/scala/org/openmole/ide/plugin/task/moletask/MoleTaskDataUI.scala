/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.moletask

import java.awt.Color
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.implementation.mole.Mole
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.core.implementation.task.MoleTask
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.misc.exception.UserBadDataError

class MoleTaskDataUI(val name: String="",
                     val mole: Option[IMoleScene] = None,
                     val finalCapsule : Option[ICapsuleUI] = None) extends TaskDataUI {

  def coreObject = mole match {
    case Some(x: IMoleScene) => 
      finalCapsule match {
        case Some(y: ICapsuleUI) =>
          //val (m: Mole,capsMap: Map[ICapsuleUI,ICapsule],_) = 
          val m : Mole = null
          val capsMap : Map[ICapsuleUI,ICapsule] = Map.empty
        //  val (m,capsMap) =  MoleMaker.buildMole(x.manager)._1
          new MoleTask(name, m,capsMap(y))
        case _ => throw new UserBadDataError("No final Capsule is set ")
      }
    case _=> throw new UserBadDataError("No Mole is set ")
  }

  def coreClass = classOf[MoleTask] 
  
  def imagePath = "img/mole.png" 
  
  override def fatImagePath = "img/mole_fat.png" 
  
  def buildPanelUI = new MoleTaskPanelUI(this)
 
  def borderColor = new Color(0,102,128)
  
  def backgroundColor = new Color(0,102,128,128)
}
