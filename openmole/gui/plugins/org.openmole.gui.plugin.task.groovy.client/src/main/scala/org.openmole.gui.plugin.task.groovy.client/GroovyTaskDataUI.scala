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

import org.openmole.gui.client.service.ClientService._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.dataui.{PrototypeDataUI, TaskDataUI}
import org.openmole.gui.plugin.task.groovy.ext.GroovyTaskData
import rx._

@JSExport("org.openmole.gui.plugin.task.groovy.client.GroovyTaskDataUI")
class GroovyTaskDataUI(val name: Var[String] = Var(""),
                       val code: Var[String] = Var(""),
                       val libs: Var[Seq[Var[String]]] = Var(Seq()),
                       val inputs: Var[Seq[Var[(PrototypeDataUI[_],Option[String])]]] = Var(Seq()),
                       val outputs: Var[Seq[Var[PrototypeDataUI[_]]]] = Var(Seq())) extends TaskDataUI{
//libs().map{c=>c()}

  def data = new GroovyTaskData(name, code, libConv(libs), inputsConv(inputs), outputsConv(outputs))

  def panelUI = new GroovyTaskPanelUI(this)
}