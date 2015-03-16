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
import org.scalajs.jquery.jQuery
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.misc.js.Forms._
import rx._

object InputFilter {
  def apply(initValue: String = "", pHolder: String = "Filter", inputID: String = filterId, size: String = "100%") = new InputFilter(initValue, pHolder, inputID, size)

  val filterId: String = "inputFilter"

  val protoFilterId1: String = "protoInputFilter1"

  val protoFilterId2: String = "protoInputFilter2"
}

import InputFilter._
class InputFilter(initValue: String, pHolder: String, inputID: String, size: String = "100%") {
  val nameFilter: Var[String] = Var("")

  val tag = bs.input(
    initValue)(
      id := inputID,
      value := initValue,
      placeholder := pHolder,
      width := size,
      autofocus
    ).render

  tag.oninput = (e: Event) ⇒ nameFilter() = tag.value

  def contains(st: String) = st.contains(nameFilter())

  def clear = {
    tag.value = ""
    nameFilter() = ""
  }

  def focus = jQuery("#" + inputID).focus
}
