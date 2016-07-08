package org.openmole.sitejs

/*
 * Copyright (C) 08/07/16 // mathieu.leclaire@openmole.org
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

import JsRxTags._
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import org.scalajs.dom
import rx._

@JSExport("Test")
object Test {
  val b: Var[Boolean] = Var(false)

  val testDiv = div(
    Rx {
      span(
        "Test div",
        backgroundColor := {
          if (b()) "green" else "red"
        },
        width := 200,
        height := 200
      )
    }
  )

  // b() = true

  @JSExport
  def render(): Unit = {
    dom.document.getElementById("openmoleTest").appendChild(testDiv.render)
  }
}
