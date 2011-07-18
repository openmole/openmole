/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.netlogo

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.netlogo.NetLogoTask
import scala.collection.JavaConversions._

class NetLogoTaskDataUI(val name: String,val workspacePath: String,val nlogoPath: String, val lauchingCommands: String) extends TaskDataUI {
  def this(n: String) = this(n,"","","")
  
  override def coreObject = new NetLogoTask(name,workspacePath,nlogoPath.split('/').toList.last,asJavaIterable(lauchingCommands.split('\n')))
  
  override def coreClass= classOf[NetLogoTask]
  
  override def imagePath = "img/thumb/netlogo.png"
  
  override def buildPanelUI = new NetLogoTaskPanelUI(this)
  
  override def borderColor = new Color(19,118,8)
  
  override def backgroundColor = new Color(175,233,175,128)
}
