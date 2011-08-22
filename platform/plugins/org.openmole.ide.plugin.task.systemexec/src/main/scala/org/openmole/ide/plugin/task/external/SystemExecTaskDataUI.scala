/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.systemexec

import java.awt.Color
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.systemexec.SystemExecTask
import scala.collection.JavaConversions._

class SystemExecTaskDataUI(val name: String,val workspace: String,val lauchingCommands: String) extends TaskDataUI {
  def this(n: String) = this(n,"","")
  
  override def coreObject = new SystemExecTask(name,lauchingCommands.filterNot(_=='\n')) {addResource(workspace)}
  
  override def coreClass= classOf[SystemExecTask]
  
  override def imagePath = "img/systemexec_task.png"
  
  override def buildPanelUI = new SystemExecTaskPanelUI(this)
  
  override def borderColor = new Color(255,200,0)
  
  override def backgroundColor = new Color(255,200,0,128)
}
