package org.openmole.gui.plugin.task.groovy.client

/*
 * Copyright (C) 26/09/14 // mathieu.leclaire@openmole.org
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

import scala.scalajs.js.annotation.JSExport

import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{Forms=> bs}
import rx._

@JSExport("org.openmole.gui.plugin.task.groovy.client.GroovyTaskPanelUI")
class GroovyTaskPanelUI(dataUI: GroovyTaskDataUI) extends PanelUI {

  val codeInput = bs.input()(placeholder := "Code").render

  @JSExport
  val view = {
    codeInput.value = dataUI.code()
    bs.div()(
      h1(id := "title", "This is a title"),
      p("GroovyTAsk !!"),
      codeInput
    )
  }

  def save = {
    dataUI.code() = codeInput.value
  }
}