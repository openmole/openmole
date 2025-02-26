package org.openmole.gui.client.tool

/*
 * Copyright (C) 14/01/16 // mathieu.leclaire@openmole.org
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

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.ext.omsheet
import com.raquo.laminar.api.L._

object OptionsDiv {

  case class BoxedOption[T](option: T, naming: T => String, checkBox: Input)

  def apply[T](options: Seq[T], naming: T => String) = new OptionsDiv(options, naming)
}

object CheckBox {
  def apply(option: String = "", default: Boolean = false, classKey: HESetters = Seq(), onchecked: Input => Unit = HTMLInputElement => {}) =
    new CheckBox(option, default, classKey, onchecked)
}

import OptionsDiv._

class OptionsDiv[T](options: Seq[T], naming: T => String) {

  val boxedOptions = options.map { o =>
    BoxedOption(o, naming, checkbox(true))
  }

  val render = div(
    paddingTop := "20",
    for {
      bo ← boxedOptions
    } yield div(
      span(omsheet.optionsdiv, naming(bo.option)),
      bo.checkBox
    )
  )

  def result: Seq[T] = boxedOptions.filter { bo =>
    bo.checkBox.ref.checked
  }.map {
    _.option
  }

}

class CheckBox(name: String, default: Boolean, classKey: HESetters, onchecked: Input => Unit) {

  private lazy val cb: Input = checkbox(default).amend(onClick --> (_ => onchecked(cb)))
  private val cbSpan = span(name, position.relative, marginRight := "5", marginLeft := "5", top := "-3")

  lazy val withNameFirst = div(classKey, cbSpan, cb)

  lazy val withBoxFirst = div(classKey, cbSpan, cb
  )

  lazy val onlyBox = div(classKey, cb)

  def result: Boolean = cb.ref.checked
}
