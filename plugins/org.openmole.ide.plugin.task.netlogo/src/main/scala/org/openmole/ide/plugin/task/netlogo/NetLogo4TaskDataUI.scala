/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo

import java.awt.Color
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo4.NetLogo4Task
import scala.collection.JavaConversions._
import scala.io.Source
import java.io.File

class NetLogo4TaskDataUI(val name: String="",
                         val workspaceEmbedded: Boolean= false,
                         val nlogoPath: String = "", 
                         val lauchingCommands: String="",
                         val prototypeMappingInput: List[(IPrototypeDataProxyUI, String)]= List(),
                         val prototypeMappingOutput: List[(String,IPrototypeDataProxyUI)]= List(),
                         val globals: List[String]= List()) extends TaskDataUI {
  
  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = 
  {
    val builder = NetLogo4Task(
      name,
      new File(nlogoPath),
      Source.fromString(lauchingCommands).getLines.toIterable,
      workspaceEmbedded)(plugins)
    builder addInput inputs
    builder addOutput outputs
    builder addParameter parameters
    prototypeMappingInput.foreach{case(p, n) => builder addNetLogoInput (p.dataUI.coreObject, n)}
    prototypeMappingOutput.foreach{case(n, p) => builder addNetLogoOutput (n, p.dataUI.coreObject)} 
    builder.toTask
  }
  
  def coreClass= classOf[NetLogo4Task]
  
  def imagePath = "img/netlogo4.png"
  
  override def fatImagePath = "img/netlogo4_fat.png"
  
  def buildPanelUI = new NetLogo4TaskPanelUI(this)
  
  def borderColor = new Color(19,118,8)
  
  def backgroundColor = new Color(175,233,175,128)
}
