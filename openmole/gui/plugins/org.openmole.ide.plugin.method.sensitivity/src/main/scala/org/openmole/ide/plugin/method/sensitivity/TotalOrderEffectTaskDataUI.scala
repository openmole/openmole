/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.awt.Color
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.method.sensitivity.TotalOrderEffectTask
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class TotalOrderEffectTaskDataUI(val name: String = "",
                                 val modelInputs: Traversable[PrototypeDataProxyUI] = List.empty,
                                 val modelOutputs: Traversable[PrototypeDataProxyUI] = List.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val builder = TotalOrderEffectTask(name,
      modelInputs.map { _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]] }.toIterable,
      modelOutputs.map { _.dataUI.coreObject.get.asInstanceOf[Prototype[Double]] }.toIterable)(plugins)
    initialise(builder)
    builder.toTask
  }

  def coreClass = classOf[TotalOrderEffectTask]

  override def imagePath = "img/totalOrderEffectTask.png"

  def fatImagePath = "img/totalOrderEffectTask_fat.png"

  def buildPanelUI = new TotalOrderEffectTaskPanelUI(this)

  def borderColor = new Color(61, 104, 130)

  def backgroundColor = new Color(61, 104, 130, 128)
}
