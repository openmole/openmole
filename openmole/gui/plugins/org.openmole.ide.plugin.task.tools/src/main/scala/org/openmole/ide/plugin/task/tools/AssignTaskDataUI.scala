/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.tools

import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.plugin.task.tools.AssignTask
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI

class AssignTaskDataUI(val name: String = "",
                       val protos: List[(PrototypeDataProxyUI, PrototypeDataProxyUI)] = List.empty,
                       val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                       val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                       val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = AssignTask(name)(plugins)
    protos.foreach {
      case (from, to) ⇒ gtBuilder.assign(from.dataUI.coreObject.get.asInstanceOf[Prototype[Any]], to.dataUI.coreObject.get.asInstanceOf[Prototype[Any]])
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[AssignTask]

  override def imagePath = "img/tools.png"

  def fatImagePath = "img/tools_fat.png"

  def buildPanelUI = new AssignTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new AssignTaskDataUI(name, protos, ins, outs, params)

}
