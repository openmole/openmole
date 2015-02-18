package org.openmole.gui.plugin.task.systemexec.client

/*
 * Copyright (C) 19/10/2014 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.ClientService
import org.openmole.gui.client.core.dataui._
import org.openmole.gui.client.core.dataui.IOMappingFactory._

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.plugin.task.systemexec.ext.SystemExecTaskData
import rx._

@JSExport("org.openmole.gui.plugin.task.systemexec.client.SystemExecTaskDataUI")
class SystemExecTaskDataUI extends TaskDataUI {
  def data = new SystemExecTaskData(inputDataUI().data.inputs, outputDataUI().data.outputs)

  def panelUI = new SystemExecTaskPanelUI(this)

  def dataType = "External"

  override def inputMappingsFactory = IOMappingsFactory(
    IOMappingFactory.defaultInputField,
    stringField("Destination", fileFilter),
    booleanField("Workdir", true, fileFilter),
    booleanField("Link", false, fileFilter)
  )

}