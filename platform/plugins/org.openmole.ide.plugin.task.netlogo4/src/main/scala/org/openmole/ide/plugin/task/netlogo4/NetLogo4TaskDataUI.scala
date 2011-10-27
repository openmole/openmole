/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo4

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo4.NetLogo4Task
import scala.collection.JavaConversions._

class NetLogo4TaskDataUI(val name: String,val workspacePath: String,val nlogoPath: String, val lauchingCommands: String) extends TaskDataUI {
  def this(n: String) = this(n,"","","")
  
  override def coreObject = new NetLogo4Task(name,workspacePath,nlogoPath.split('/').toList.last,asJavaIterable(lauchingCommands.split('\n')))
  
  override def coreClass= classOf[NetLogo4Task]
  
  override def imagePath = "img/netlogo4.png"
  
  override def buildPanelUI = new NetLogo4TaskPanelUI(this)
  
  override def borderColor = new Color(19,118,8)
  
  override def backgroundColor = new Color(175,233,175,128)
}
