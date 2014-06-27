/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo

import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo5.NetLogo5Task
import scala.io.Source
import java.io.File
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }
import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.misc.tools.util.Converters._

@deprecated("NetLogo5TaskDataUI010 is now used", "0.10")
class NetLogo5TaskDataUI(val name: String = "",
                         val workspaceEmbedded: Boolean = false,
                         val nlogoPath: String = "",
                         val lauchingCommands: String = "",
                         val prototypeMappingInput: List[(PrototypeDataProxyUI, String)] = List(),
                         val prototypeMappingOutput: List[(String, PrototypeDataProxyUI)] = List(),
                         val resources: List[String] = List(),
                         val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends Update[NetLogo5TaskDataUI010] {
  def update = new NetLogo5TaskDataUI010(name,
    FilledWorkspace(Right(nlogoPath)),
    lauchingCommands,
    prototypeMappingInput,
    prototypeMappingOutput,
    resources,
    inputs,
    outputs,
    inputParameters)
}

@deprecated("NetLogo5TaskDataUI1 is now used", "1.0")
class NetLogo5TaskDataUI010(val name: String = "",
                            val workspace: FilledWorkspace = FilledWorkspace(Right("")),
                            val lauchingCommands: String = "",
                            val prototypeMappingInput: List[(PrototypeDataProxyUI, String, Int)] = List(),
                            val prototypeMappingOutput: List[(String, PrototypeDataProxyUI, Int)] = List(),
                            val resources: List[String] = List(),
                            val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                            val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                            val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends Update[NetLogo5TaskDataUI1] {
  def update = {
    new NetLogo5TaskDataUI1(name,
      workspace,
      lauchingCommands,
      prototypeMappingInput,
      prototypeMappingOutput,
      resources.map { new File(_) },
      inputs,
      outputs,
      inputParameters)
  }
}
class NetLogo5TaskDataUI1(val name: String = "",
                          val workspace: Workspace = EmptyWorkspace,
                          val lauchingCommands: String = "",
                          val prototypeMappingInput: List[(PrototypeDataProxyUI, String, Int)] = List(),
                          val prototypeMappingOutput: List[(String, PrototypeDataProxyUI, Int)] = List(),
                          val resources: List[File] = List(),
                          val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                          val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                          val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {
  def coreObject(plugins: PluginSet) = util.Try {
    val builder = NetLogo5Task(
      name,
      Workspace.toCoreObject(workspace),
      Source.fromString(lauchingCommands).getLines.toIterable)(plugins)
    initialise(builder)
    resources.foreach {
      r ⇒ builder addResource r
    }
    prototypeMappingInput.foreach {
      case (p, n, _) ⇒ builder addNetLogoInput (p.dataUI.coreObject.get, n)
    }
    prototypeMappingOutput.foreach {
      case (n, p, _) ⇒ builder addNetLogoOutput (n, p.dataUI.coreObject.get)
    }
    builder.toTask
  }

  def coreClass = classOf[NetLogo5Task]

  override def imagePath = "img/netlogo5.png"

  def fatImagePath = "img/netlogo5_fat.png"

  def buildPanelUI = new NetLogo5TaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new NetLogo5TaskDataUI1(name,
    workspace,
    lauchingCommands,
    Proxies.instance.filterListTupleIn(prototypeMappingInput),
    Proxies.instance.filterListTupleOut(prototypeMappingOutput),
    resources,
    ins,
    outs,
    params)
}
