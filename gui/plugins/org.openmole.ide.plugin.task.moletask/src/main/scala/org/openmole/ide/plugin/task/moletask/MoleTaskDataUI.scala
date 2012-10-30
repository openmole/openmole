/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.moletask

import java.awt.Color
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.core.implementation.task.MoleTask
import org.openmole.ide.core.implementation.workflow.MoleSceneManager
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.misc.exception.UserBadDataError
import scala.collection.JavaConversions._

object MoleTaskDataUI {
  def manager(i: Int): Option[IMoleSceneManager] = ScenesManager.moleScenes.map { _.manager }.filter { _.id == i }.headOption
  def capsule(t: ITaskDataProxyUI, manager: IMoleSceneManager): Option[ICapsuleDataUI] =
    manager.capsules.values.map { _.dataUI }.filter { _.task.isDefined }.filter { _.task.get == t }.headOption
  def emptyMoleSceneManager = new MoleSceneManager("")
}

import MoleTaskDataUI._
class MoleTaskDataUI(val name: String = "",
                     val mole: Option[Int] = None,
                     val finalCapsule: Option[ITaskDataProxyUI] = None) extends TaskDataUI {

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = mole match {
    case Some(x: Int) ⇒ manager(x) match {
      case Some(y: IMoleSceneManager) ⇒
        finalCapsule match {
          case Some(z: ITaskDataProxyUI) ⇒
            MoleTaskDataUI.capsule(z, y) match {
              case Some(w: ICapsuleDataUI) ⇒
                MoleMaker.buildMole(y) match {
                  case Right((m, capsMap, protoMap, errs)) ⇒
                    val builder = MoleTask(name, m, capsMap.find { case (k, _) ⇒ k.dataUI == w }.get._2)(plugins)
                    builder addInput inputs
                    builder addOutput outputs
                    builder addParameter parameters
                    builder.toTask
                  case Left(l) ⇒ throw new UserBadDataError(l)
                }
              case _ ⇒ throw new UserBadDataError("No final Capsule is set in the " + name + "Task")
            }
          case _ ⇒ throw new UserBadDataError("A capsule (in the " + name + "Task) without task can not be run")
        }
      case _ ⇒ throw new UserBadDataError("No Mole is set in the " + name + "Task")
    }
    case _ ⇒ throw new UserBadDataError("No Mole is set in the " + name + "Task")
  }

  def coreClass = classOf[MoleTask]

  override def imagePath = "img/mole.png"

  override def fatImagePath = "img/mole_fat.png"

  def buildPanelUI = new MoleTaskPanelUI(this)
}
