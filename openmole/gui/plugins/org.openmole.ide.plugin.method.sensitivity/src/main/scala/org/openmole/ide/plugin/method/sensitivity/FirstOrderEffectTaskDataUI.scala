/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.method.sensitivity.FirstOrderEffectTask
import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }

class FirstOrderEffectTaskDataUI(val name: String = "",
                                 val modelInputs: Traversable[PrototypeDataProxyUI] = List.empty,
                                 val modelOutputs: Traversable[PrototypeDataProxyUI] = List.empty,
                                 val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                                 val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                                 val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val builder =
      FirstOrderEffectTask(
        name,
        modelInputs.map {
          _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]]
        }.toIterable,
        modelOutputs.map {
          _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]]
        }.toIterable)(plugins)
    initialise(builder)
    builder.toTask
  }

  def coreClass = classOf[FirstOrderEffectTask]

  override def imagePath = "img/firstOrderEffectTask.png"

  def fatImagePath = "img/firstOrderEffectTask_fat.png"

  def buildPanelUI = new FirstOrderEffectTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new FirstOrderEffectTaskDataUI(name,
    Proxies.instance.filter(modelInputs.toList),
    Proxies.instance.filter(modelOutputs.toList),
    ins,
    outs,
    params)

}
