/*
 * Copyright (C) 2013 Mathieu Leclaire
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.template

import java.io.File
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.plugin.task.template.TemplateFileTask
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype

class TemplateTaskDataUI(val name: String = "",
                         val template: String = "",
                         val output: Option[PrototypeDataProxyUI] = None,
                         val inputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val outputs: Seq[PrototypeDataProxyUI] = Seq.empty,
                         val inputParameters: Map[PrototypeDataProxyUI, String] = Map.empty) extends TaskDataUI {

  def coreObject(plugins: PluginSet) = util.Try {
    output match {
      case None ⇒ throw new UserBadDataError("An output prototype is required")
      case Some(x: PrototypeDataProxyUI) ⇒
        val gtBuilder = TemplateFileTask(name, new File(template), x.dataUI.coreObject.get.asInstanceOf[Prototype[File]])(plugins)
        initialise(gtBuilder)
        gtBuilder.toTask
    }
  }

  def coreClass = classOf[TemplateFileTask]

  override def imagePath = "img/templateTask.png"

  def fatImagePath = "img/templateTask_fat.png"

  def buildPanelUI = new TemplateTaskPanelUI(this)

  def doClone(ins: Seq[PrototypeDataProxyUI],
              outs: Seq[PrototypeDataProxyUI],
              params: Map[PrototypeDataProxyUI, String]) = new TemplateTaskDataUI(name, template, output, ins, outs, params)

}
