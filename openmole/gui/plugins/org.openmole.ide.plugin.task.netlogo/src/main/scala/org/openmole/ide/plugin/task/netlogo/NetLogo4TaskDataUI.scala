/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo4.NetLogo4Task
import scala.collection.JavaConversions._
import scala.io.Source
import java.io.File

case class NetLogo4TaskDataUI(
    name: String = "",
    workspaceEmbedded: Boolean = false,
    nlogoPath: String = "",
    lauchingCommands: String = "",
    prototypeMappingInput: List[(IPrototypeDataProxyUI, String)] = List(),
    prototypeMappingOutput: List[(String, IPrototypeDataProxyUI)] = List(),
    resources: List[String] = List()) extends TaskDataUI {

  override def cloneWithoutPrototype(proxy: IPrototypeDataProxyUI) =
    this.copy(prototypeMappingInput = prototypeMappingInput.filterNot(_._1 == proxy),
      prototypeMappingOutput = prototypeMappingOutput.filterNot(_._2 == proxy))

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) =
    {
      val builder = NetLogo4Task(
        name,
        new File(nlogoPath),
        Source.fromString(lauchingCommands).getLines.toIterable,
        workspaceEmbedded)(plugins)
      builder addInput inputs
      builder addOutput outputs
      builder addParameter parameters
      resources.foreach { r ⇒ builder addResource (new File(r)) }
      prototypeMappingInput.foreach { case (p, n) ⇒ builder addNetLogoInput (p.dataUI.coreObject, n) }
      prototypeMappingOutput.foreach { case (n, p) ⇒ builder addNetLogoOutput (n, p.dataUI.coreObject) }
      builder.toTask
    }

  def coreClass = classOf[NetLogo4Task]

  override def imagePath = "img/netlogo4.png"

  def fatImagePath = "img/netlogo4_fat.png"

  def buildPanelUI = new NetLogo4TaskPanelUI(this)

}
