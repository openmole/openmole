package org.openmole.gui.misc.js

/*
 * Copyright (C) 05/02/15 // mathieu.leclaire@openmole.org
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

import org.scalajs.dom.Event

import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{ Forms ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.Forms._
import rx._

class InputFilter(initValue: String) {
  var nameFilter = Var("")

  val tag = bs.input(
    initValue)(
      value := initValue,
      placeholder := "Filter",
      autofocus := "true"
    ).render

  tag.oninput = (e: Event) ⇒ nameFilter() = tag.value

  def contains(st: String) = st.contains(nameFilter())
}
