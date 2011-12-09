/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo5

import java.awt.Color
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo5.NetLogo5Task
import scala.collection.JavaConversions._
import scala.io.Source

class NetLogo5TaskDataUI(val name: String,
                         val workspaceEmbedded: Boolean=false,
                         val nlogoPath: String = "", 
                         val lauchingCommands: String = "",
                         var prototypeMappingInput: List[(IPrototypeDataProxyUI, String)]= List(),
                         var prototypeMappingOutput: List[(String,IPrototypeDataProxyUI)] = List(),
                         val globals: List[String] = List()) extends TaskDataUI {
  
  override def coreObject = {
    val nlt = new NetLogo5Task(name,
                               nlogoPath.split('/').toList.last,
                               asJavaIterable(Source.fromString(lauchingCommands).getLines.toIterable),
                               workspaceEmbedded)
    prototypeMappingInput.foreach(pm=>nlt.addNetLogoInput(pm._1.dataUI.coreObject,pm._2))
    prototypeMappingOutput.foreach(pm=>nlt.addNetLogoOutput(pm._1,pm._2.dataUI.coreObject))
    nlt
  }
  
  override def coreClass= classOf[NetLogo5Task]
  
  override def imagePath = "img/netlogo5.png"
  
  override def buildPanelUI = new NetLogo5TaskPanelUI(this)
  
  override def borderColor = new Color(19,118,8)
  
  override def backgroundColor = new Color(175,233,175,128)
}
