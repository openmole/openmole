/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.tools

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.plugin.task.tools.FlattenTask

class FlattenTaskDataUI[T](val name: String = "",
                           val protos: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty,
                           val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                           val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = FlattenTask(name)(plugins)
    protos.foreach {
      case (from, to) ⇒ gtBuilder.flatten(from.dataUI.coreObject.get.asInstanceOf[Prototype[Array[Array[T]]]], to.dataUI.coreObject.get.asInstanceOf[Prototype[Array[T]]])
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[FlattenTask]

  override def imagePath = "img/tools.png"

  def fatImagePath = "img/tools_fat.png"

  def buildPanelUI = new FlattenTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new FlattenTaskDataUI(name, protos, ins, outs, params)

}
