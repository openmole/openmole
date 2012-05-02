/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Color
import java.io.File
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskDataUI(val name: String="",
                       val code: String="",
                       val libs: List[String]= List.empty,
                       val plugins: List[String]= List.empty) extends TaskDataUI {
  
  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
    val gtBuilder = GroovyTask(name,code, libs.map{l => new File(l)})(plugins ++ this.plugins.map{l => new File(l)})
    gtBuilder addInput inputs
    gtBuilder addOutput outputs
    gtBuilder addParameter parameters
    gtBuilder.toTask
  }
  
  def coreClass= classOf[GroovyTask]
  
  override def imagePath = "img/groovyTask.png"
  
  def fatImagePath = "img/groovyTask_fat.png"
  
  def buildPanelUI = new GroovyTaskPanelUI(this)
  
  def borderColor = new Color(61,104,130)
  
  def backgroundColor = new Color(61,104,130,128)
}
