/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.systemexec

import java.io.File
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.systemexec.SystemExecTask
import scala.collection.JavaConversions._
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }

class SystemExecTaskDataUI(val name: String = "",
                           val workdir: String = "",
                           val launchingCommands: String = "",
                           val resources: List[String] = List.empty,
                           val inputMap: List[(PrototypeDataProxyUI, String)] = List.empty,
                           val outputMap: List[(String, PrototypeDataProxyUI)] = List.empty,
                           val variables: List[PrototypeDataProxyUI] = List.empty,
                           val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val syet = SystemExecTask(name, directory = workdir)(plugins)
    syet command launchingCommands.filterNot(_ == '\n')
    initialise(syet)
    resources.foreach(syet addResource new File(_))
    variables.foreach {
      p ⇒ syet addVariable (p.dataUI.coreObject.get)
    }

    outputMap.foreach(i ⇒ syet addOutput (i._1, i._2.dataUI.coreObject.get.asInstanceOf[Prototype[File]]))
    inputMap.foreach(i ⇒ syet addInput (i._1.dataUI.coreObject.get.asInstanceOf[Prototype[File]], i._2))
    syet
  }

  def coreClass = classOf[SystemExecTask]

  override def imagePath = "img/systemexec_task.png"

  def fatImagePath = "img/systemexec_task_fat.png"

  def buildPanelUI = new SystemExecTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new SystemExecTaskDataUI(name,
    workdir,
    launchingCommands,
    resources,
    Proxies.instance.filterListTupleIn(inputMap),
    Proxies.instance.filterListTupleOut(outputMap),
    Proxies.instance.filter(variables),
    ins,
    outs,
    params)

}
