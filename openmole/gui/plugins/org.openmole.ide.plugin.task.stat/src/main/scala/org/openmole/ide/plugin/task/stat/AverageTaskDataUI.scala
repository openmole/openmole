/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.stat

import java.awt.Color
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.plugin.task.stat.AverageTask

class AverageTaskDataUI(val name: String = "",
                        val sequence: List[(IPrototypeDataProxyUI, IPrototypeDataProxyUI)] = List.empty) extends StatDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = AverageTask(name)(plugins)

    sequence foreach { s ⇒
      gtBuilder addSequence (s._1.dataUI.coreObject.asInstanceOf[Prototype[Array[Double]]],
        s._2.dataUI.coreObject.asInstanceOf[Prototype[Double]])
    }

    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[AverageTask]

  def fatImagePath = "img/average_fat.png"

  override def imagePath = "img/average.png"

  def buildPanelUI = new AverageTaskPanelUI(this)
}
