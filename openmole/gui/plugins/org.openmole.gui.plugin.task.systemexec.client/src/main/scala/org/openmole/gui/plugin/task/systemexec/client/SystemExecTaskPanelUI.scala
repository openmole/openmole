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

import org.openmole.gui.ext.dataui.PanelUI

import org.scalajs.dom
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.tags._
import scalatags.JsDom.tags2._
import scalatags.JsDom.attrs._
import scalatags.JsDom.short._
import scalatags.generic.TypedTag
import org.openmole.gui.misc.js.Forms._

@JSExport("org.openmole.gui.plugin.task.systemexec.client.SystemExecTaskPanelUI")
class SystemExecTaskPanelUI(dataUI: SystemExecTaskDataUI) extends PanelUI {

  type DATAUI = SystemExecTaskDataUI

  val tag = div(
    h1(id := "title", "This is a title"),
    p("SystemExecTask !")
  )

  @JSExport
  def view: FormTag = tag

  def save(name: String) = new SystemExecTaskDataUI
}