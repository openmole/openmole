/*
 * Copyright (C) 2013 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.imagej

import java.io.File
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.plugin.task.groovy.GroovyTask
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.core.implementation.task.EmptyTask

class ImageJTaskDataUI(val name: String = "",
                       val code: String = "",
                       val libs: List[String] = List.empty,
                       val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                       val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                       val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    val gtBuilder = GroovyTask(name, code)(plugins)
    libs.foreach {
      l ⇒ gtBuilder.addLib(new File(l))
    }
    initialise(gtBuilder)
    gtBuilder.toTask
  }

  def coreClass = classOf[EmptyTask]

  override def imagePath = "img/imagej.png"

  def fatImagePath = "img/imagej_fat.png"

  def buildPanelUI = new ImageJTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new ImageJTaskDataUI(name, code, libs, ins, outs, params)

}
