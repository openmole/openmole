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

class NetLogo5TaskDataUI(val name: String,
                         val workspacePath: String,
                         val nlogoPath: String, 
                         val lauchingCommands: String,
                         var prototypeMappingInput: List[(IPrototypeDataProxyUI, String)],
                         var prototypeMappingOutput: List[(String,IPrototypeDataProxyUI)],
                         val globals: List[String]) extends TaskDataUI {
  def this(n: String) = this(n,"","","",List(),List(),List())
  
  override def coreObject = {
    val nlt = new NetLogo5Task(name,
                           workspacePath,nlogoPath.split('/').toList.last,
                           asJavaIterable(lauchingCommands.split('\n')))
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
