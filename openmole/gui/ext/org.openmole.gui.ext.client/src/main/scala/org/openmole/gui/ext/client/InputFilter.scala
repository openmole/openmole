package org.openmole.gui.ext.client

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
import rx._
import scaladget.bootstrapnative.bsn._
import scalatags.JsDom.all._

object InputFilter {
  def apply(initValue: String = "", pHolder: String = "Filter", size: String = "100%") = new InputFilter(initValue, pHolder, size)
}

class InputFilter(initValue: String, pHolder: String, size: String = "100%") {
  val nameFilter: Var[String] = Var("")

  val tag = inputTag(
    initValue
  )(
    value := initValue,
    placeholder := pHolder,
    width := size,
    autofocus
  ).render

  tag.oninput = (e: Event) ⇒ nameFilter() = tag.value

  def contains(st: String) = st.toUpperCase.contains(nameFilter.now.toUpperCase)

  def exists(seqString: Seq[String]) = seqString.exists(contains)

  def clear = {
    tag.value = ""
    nameFilter() = ""
  }
}
