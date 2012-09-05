/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.awt.Color
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.method.sensitivity.FirstOrderEffectTask

class FirstOrderEffectTaskDataUI(val name: String = "",
                                 val modelInputs: Iterable[IPrototypeDataProxyUI] = List.empty,
                                 val modelOutputs: Iterable[IPrototypeDataProxyUI] = List.empty) extends TaskDataUI {

  def coreObject(inputs: DataSet, outputs: DataSet, parameters: ParameterSet, plugins: PluginSet) = {
    val builder = FirstOrderEffectTask(name,
      modelInputs.map { _.dataUI.coreObject.asInstanceOf[Prototype[Double]] },
      modelOutputs.map { _.dataUI.coreObject.asInstanceOf[Prototype[Double]] })(plugins)
    builder addOutput inputs
    builder addOutput outputs
    builder addParameter parameters
    builder.toTask
  }

  def coreClass = classOf[FirstOrderEffectTask]

  override def imagePath = "img/firstOrderEffectTask.png"

  def fatImagePath = "img/firstOrderEffectTask_fat.png"

  def buildPanelUI = new FirstOrderEffectTaskPanelUI(this)

  def borderColor = new Color(61, 104, 130)

  def backgroundColor = new Color(61, 104, 130, 128)
}
