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
import org.openmole.plugin.task.stat.MedianTask

class MedianTaskDataUI(val name: String = "",
                       val sequence: List[(IPrototypeDataProxyUI, IPrototypeDataProxyUI)] = List.empty) extends TaskDataUI {

  def coreObject(inputs: IDataSet, outputs: IDataSet, parameters: IParameterSet, plugins: IPluginSet) = {
    val gtBuilder = MedianTask(name)(plugins)
    sequence foreach { s ⇒
      gtBuilder addSequence (s._1.dataUI.coreObject.asInstanceOf[IPrototype[Array[Double]]],
        s._2.dataUI.coreObject.asInstanceOf[IPrototype[Double]])
    }
    gtBuilder addInput inputs
    gtBuilder addOutput outputs
    gtBuilder addParameter parameters
    gtBuilder.toTask
  }

  def coreClass = classOf[MedianTask]

  override def imagePath = "img/median.png"

  def fatImagePath = "img/median_fat.png"

  def buildPanelUI = new MedianTaskPanelUI(this)

  def borderColor = new Color(61, 104, 130)

  def backgroundColor = new Color(61, 104, 130, 128)
}
