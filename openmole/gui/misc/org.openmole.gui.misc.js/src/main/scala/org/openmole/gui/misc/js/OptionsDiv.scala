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

import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import org.scalajs.dom.raw.HTMLInputElement
import scalatags.JsDom.{tags ⇒ tags}
import scalatags.JsDom.all._
import bs._

object OptionsDiv {
  def apply[T <: Displayable](options: Seq[T]) = new OptionsDiv(options)
}

class OptionsDiv[T <: Displayable](options: Seq[T]) {

  case class BoxedOption(option: T, checkBox: HTMLInputElement)

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

  def result: Seq[T] = boxedOptions.filter { bo=>
    println("RES " + bo.option.name + " :" + bo.checkBox.checked)
    bo.checkBox.checked
  }.map {
    _.option
  }
}
