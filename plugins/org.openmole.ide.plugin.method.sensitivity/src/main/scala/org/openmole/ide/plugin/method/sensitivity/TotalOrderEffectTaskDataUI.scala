/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.awt.Color
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.method.sensitivity.TotalOrderEffectTask

class TotalOrderEffectTaskDataUI(val name: String="",
                                 val modelInputs : Iterable[IPrototypeDataProxyUI] = List.empty,
                                 val modelOutputs: Iterable[IPrototypeDataProxyUI] = List.empty) extends TaskDataUI {
  
  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
    val builder = TotalOrderEffectTask(name,
                                       modelInputs.map{_.dataUI.coreObject.asInstanceOf[IPrototype[Double]]},
                                       modelOutputs.map{_.dataUI.coreObject.asInstanceOf[IPrototype[Double]]})(plugins)
    builder addOutput inputs         
    builder addOutput outputs
    builder addParameter parameters
    builder.toTask
  }
  
  def coreClass= classOf[TotalOrderEffectTask]
  
  override def imagePath = "img/totalOrderEffectTask.png"
  
  def fatImagePath = "img/totalOrderEffectTask_fat.png"
  
  def buildPanelUI = new TotalOrderEffectTaskPanelUI(this)
  
  def borderColor = new Color(61,104,130)
  
  def backgroundColor = new Color(61,104,130,128)
}
