package org.openmole.gui.misc.js

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

import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs, ClassKeyAggregator}
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.HTMLInputElement
import scalatags.JsDom.{tags ⇒ tags}
import scalatags.JsDom.all._
import bs._

object OptionsDiv {

  case class BoxedOption[T <: Displayable](option: T, checkBox: HTMLInputElement)

  def apply[T <: Displayable](options: Seq[T]) = new OptionsDiv(options)
}

object CheckBox {
  def apply(option: String = "", default: Boolean = false, classKey: ClassKeyAggregator = emptyCK)(onchecked: HTMLInputElement => Unit = HTMLInputElement=> {}) =
    new CheckBox(option, default, classKey)(onchecked)
}

import OptionsDiv._

class OptionsDiv[T <: Displayable](options: Seq[T]) {

  val boxedOptions = options.map { o =>
    BoxedOption(o, bs.checkbox(true).render)
  }

  val div = bs.div("spacer20")(
    for {
      bo <- boxedOptions
    } yield tags.div(
      bs.span("options")(bo.option.name),
      bo.checkBox
    )
  )

  def result: Seq[T] = boxedOptions.filter { bo =>
    bo.checkBox.checked
  }.map {
    _.option
  }

}

class CheckBox(name: String, default: Boolean, classKey: ClassKeyAggregator)(onchecked: HTMLInputElement => Unit) {

  private lazy val cb: Input = tags.input(`type` := "checkbox", if (default) checked := true else "", onclick := { () ⇒ onchecked(cb) }).render
  private val cbSpan = tags.span(name, style := "position: relative; margin-right:5px; margin-left:5px; top: -3px;")

  lazy val withNameFirst = bs.div(classKey)(
    cbSpan,
    cb
  )

  lazy val withBoxFirst = bs.div(classKey)(
    cbSpan,
    cb
  )

  lazy val onlyBox = bs.div(classKey)(
    cb
  )

  def result: Boolean = cb.checked
}
