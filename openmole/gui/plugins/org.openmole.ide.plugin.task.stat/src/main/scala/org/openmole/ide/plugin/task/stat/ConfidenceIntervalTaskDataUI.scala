/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.stat

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }
import org.openmole.core.model.task.PluginSet
import org.openmole.plugin.task.stat.ConfidenceIntervalTask
import org.openmole.core.model.data.Prototype

class ConfidenceIntervalTaskDataUI(val name: String = "",
                                   val sequence: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty,
                                   val level: Double = 0.95,
                                   val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                                   val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                                   val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends StatDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = ConfidenceIntervalTask(name, level)(plugins)
    sequence foreach {
      s ⇒
        gtBuilder addSequence (s._1.dataUI.coreObject.get.asInstanceOf[Prototype[Array[Double]]],
          s._2.dataUI.coreObject.get.asInstanceOf[Prototype[Double]])
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[ConfidenceIntervalTask]

  override def imagePath = "img/ci.png"

  def fatImagePath = "img/ci_fat.png"

  def buildPanelUI = new ConfidenceIntervalTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new ConfidenceIntervalTaskDataUI(name, Proxies.instance.filterListTupleInOut(sequence), level, ins, outs, params)
}