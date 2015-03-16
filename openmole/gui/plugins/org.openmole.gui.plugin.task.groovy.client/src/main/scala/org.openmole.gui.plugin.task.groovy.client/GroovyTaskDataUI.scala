package org.openmole.gui.plugin.task.groovy.client

/*
 * Copyright (C) 25/09/14 // mathieu.leclaire@openmole.org
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
import org.openmole.gui.client.core.dataui.{IOMappingsFactory, TaskDataUI}
import ClientService._
import org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData
import rx._
import org.openmole.gui.client.core.dataui.IOMappingFactory._

class GroovyTaskDataUI(val code: Var[String] = Var(""),
                       val libs: Var[Seq[Var[String]]] = Var(Seq())) extends TaskDataUI {
//libs().map{c=>c()}

  def data = new GroovyTaskData(inputDataUI().data.inputs, outputDataUI().data.outputs, code, libs)

  def panelUI = new GroovyTaskPanelUI(this)

  def dataType = "Groovy"
}