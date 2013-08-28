/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.awt.Color
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.method.sensitivity.FirstOrderEffectTask
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class FirstOrderEffectTaskDataUI(val name: String = "",
                                 val modelInputs: Traversable[PrototypeDataProxyUI] = List.empty,
                                 val modelOutputs: Traversable[PrototypeDataProxyUI] = List.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val builder =
      FirstOrderEffectTask(
        name,
        modelInputs.map { _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]] }.toIterable,
        modelOutputs.map { _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]] }.toIterable)(plugins)
    initialise(builder)
    builder.toTask
  }

  def coreClass = classOf[FirstOrderEffectTask]

  override def imagePath = "img/firstOrderEffectTask.png"

  def fatImagePath = "img/firstOrderEffectTask_fat.png"

  def buildPanelUI = new FirstOrderEffectTaskPanelUI(this)

  def borderColor = new Color(61, 104, 130)

  def backgroundColor = new Color(61, 104, 130, 128)
}
