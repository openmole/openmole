/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.systemexec

import java.awt.Color
import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.task.systemexec.SystemExecTask
import scala.collection.JavaConversions._

class SystemExecTaskDataUI(val name: String="",
                           val workdir: String="",
                           val lauchingCommands: String="", 
                           val resources: List[String]= List.empty,
                           val inputMap: List[(IPrototypeDataProxyUI,String)]=List.empty,
                           val outputMap: List[(String,IPrototypeDataProxyUI)]= List.empty) extends TaskDataUI {
  
  override def coreObject = {
    val syet = new SystemExecTask(name,lauchingCommands.filterNot(_=='\n'),workdir)
    resources.foreach(syet.addResource)
    outputMap.foreach(i=>syet.addOutput(i._1,i._2.dataUI.coreObject.asInstanceOf[IPrototype[File]]))
    inputMap.foreach(i=>syet.addInput(i._1.dataUI.coreObject.asInstanceOf[IPrototype[File]],i._2))
    syet
  }
  
  override def coreClass= classOf[SystemExecTask]
  
  override def imagePath = "img/systemexec_task.png"
  
  override def buildPanelUI = new SystemExecTaskPanelUI(this)
  
  override def borderColor = new Color(255,200,0)
  
  override def backgroundColor = new Color(255,200,0,128)
}
