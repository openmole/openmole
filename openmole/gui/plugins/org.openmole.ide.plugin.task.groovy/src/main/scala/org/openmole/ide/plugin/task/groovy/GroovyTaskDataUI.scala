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
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.serializer.Update

class GroovyTaskDataUI(val name: String,
                       val code: String,
                       val libs: List[String],
                       val inputs: Seq[PrototypeDataProxyUI],
                       val outputs: Seq[PrototypeDataProxyUI],
                       val inputParameters: Map[PrototypeDataProxyUI, String]) extends Update[GroovyTaskDataUI010] {
  def update = new GroovyTaskDataUI010(name, code, libs.map { l ⇒ new File(l) }, inputs, outputs, inputParameters)
}

class GroovyTaskDataUI010(val name: String = "",
                          val code: String = "",
                          val libs: List[File] = List.empty,
                          val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                          val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                          val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = GroovyTask(name, code)(plugins)
    libs.foreach {
      l ⇒ gtBuilder.addLib(l)
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[GroovyTask]

  override def imagePath = "img/groovyTask.png"

  def fatImagePath = "img/groovyTask_fat.png"

  def buildPanelUI = new GroovyTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new GroovyTaskDataUI010(name, code, libs, ins, outs, params)

}
