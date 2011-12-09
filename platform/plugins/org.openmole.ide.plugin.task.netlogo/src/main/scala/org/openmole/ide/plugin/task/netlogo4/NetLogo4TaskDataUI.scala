/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo4

import java.awt.Color
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo4.NetLogo4Task
import scala.collection.JavaConversions._
import scala.io.Source
import java.io.File

class NetLogo4TaskDataUI(val name: String,
                         val workspaceEmbedded: Boolean= false,
                         val nlogoPath: String = "", 
                         val lauchingCommands: String="",
                         val prototypeMappingInput: List[(IPrototypeDataProxyUI, String)]= List(),
                         val prototypeMappingOutput: List[(String,IPrototypeDataProxyUI)]= List(),
                         val globals: List[String]= List()) extends TaskDataUI {
  
  override def coreObject = {val nlt = new NetLogo4Task(name,
                                                        new File(nlogoPath),
                                                        asJavaIterable(Source.fromString(lauchingCommands).getLines.toIterable),
                                                        workspaceEmbedded)
                             prototypeMappingInput.foreach(pm=>nlt.addNetLogoInput(pm._1.dataUI.coreObject,pm._2))
                             prototypeMappingOutput.foreach(pm=>nlt.addNetLogoOutput(pm._1,pm._2.dataUI.coreObject))
                             nlt
  }
  override def coreClass= classOf[NetLogo4Task]
  
  override def imagePath = "img/netlogo4.png"
  
  override def buildPanelUI = new NetLogo4TaskPanelUI(this)
  
  override def borderColor = new Color(19,118,8)
  
  override def backgroundColor = new Color(175,233,175,128)
}
