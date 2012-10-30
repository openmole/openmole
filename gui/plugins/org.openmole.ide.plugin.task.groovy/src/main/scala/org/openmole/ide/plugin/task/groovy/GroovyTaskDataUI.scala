/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import java.io.File
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.groovy.GroovyTask

class GroovyTaskDataUI(val name: String = "",
                       val code: String = "",
                       val libs: List[String] = List.empty) extends TaskDataUI {

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = {
    val gtBuilder = GroovyTask(name, code)(plugins)
    libs.foreach { l ⇒ gtBuilder.addLib(new File(l)) }
    gtBuilder addInput inputs
    gtBuilder addOutput outputs
    gtBuilder addParameter parameters
    gtBuilder.toTask
  }

  def coreClass = classOf[GroovyTask]

  override def imagePath = "img/groovyTask.png"

  def fatImagePath = "img/groovyTask_fat.png"

  def buildPanelUI = new GroovyTaskPanelUI(this)
}
