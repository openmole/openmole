/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.stat

import java.awt.Color
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.task.stat.SumTask

class SumTaskDataUI(val name: String = "",
                    val sequence: List[(IPrototypeDataProxyUI, IPrototypeDataProxyUI)] = List.empty) extends TaskDataUI {

  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
    val gtBuilder = SumTask(name)(plugins)
    sequence foreach { s ⇒
      gtBuilder addSequence (s._1.dataUI.coreObject.asInstanceOf[IPrototype[Array[Double]]],
        s._2.dataUI.coreObject.asInstanceOf[IPrototype[Double]])
    }
    gtBuilder addInput inputs
    gtBuilder addOutput outputs
    gtBuilder addParameter parameters
    gtBuilder.toTask
  }
  
  def coreClass= classOf[SumTask]
  
  override def imagePath = "img/sum.png"
  
  def fatImagePath = "img/sum_fat.png"
  
  def buildPanelUI = new SumTaskPanelUI(this)

  def borderColor = new Color(61, 104, 130)

  def backgroundColor = new Color(61, 104, 130, 128)
}
