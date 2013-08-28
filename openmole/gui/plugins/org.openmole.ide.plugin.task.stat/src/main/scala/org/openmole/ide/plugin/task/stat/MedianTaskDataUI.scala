/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.stat

import java.awt.Color
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.plugin.task.stat.MedianTask
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class MedianTaskDataUI(val name: String = "",
                       val sequence: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty) extends StatDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = MedianTask(name)(plugins)
    sequence foreach { s ⇒
      gtBuilder addSequence (s._1.dataUI.coreObject.get.asInstanceOf[Prototype[Array[Double]]],
        s._2.dataUI.coreObject.get.asInstanceOf[Prototype[Double]])
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[MedianTask]

  override def imagePath = "img/median.png"

  def fatImagePath = "img/median_fat.png"

  def buildPanelUI = new MedianTaskPanelUI(this)
}
