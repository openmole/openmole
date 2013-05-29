/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.systemexec

import java.io.File
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.task.systemexec.SystemExecTask
import scala.collection.JavaConversions._

class SystemExecTaskDataUI(val name: String = "",
                           val workdir: String = "",
                           val lauchingCommands: String = "",
                           val resources: List[String] = List.empty,
                           val inputMap: List[(IPrototypeDataProxyUI, String)] = List.empty,
                           val outputMap: List[(String, IPrototypeDataProxyUI)] = List.empty,
                           val variables: List[IPrototypeDataProxyUI] = List.empty) extends TaskDataUI {

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = {
    val syet = SystemExecTask(name, directory = workdir)(plugins)
    syet command lauchingCommands.filterNot(_ == '\n')
    syet addInput inputs
    syet addOutput outputs
    syet addParameter parameters
    resources.foreach(syet addResource new File(_))
    variables.foreach { p ⇒ syet addVariable (p.dataUI.coreObject) }

    outputMap.foreach(i ⇒ syet addOutput (i._1, i._2.dataUI.coreObject.asInstanceOf[Prototype[File]]))
    inputMap.foreach(i ⇒ syet addInput (i._1.dataUI.coreObject.asInstanceOf[Prototype[File]], i._2))
    syet
  }

  def coreClass = classOf[SystemExecTask]

  override def imagePath = "img/systemexec_task.png"

  def fatImagePath = "img/systemexec_task_fat.png"

  def buildPanelUI = new SystemExecTaskPanelUI(this)
}
