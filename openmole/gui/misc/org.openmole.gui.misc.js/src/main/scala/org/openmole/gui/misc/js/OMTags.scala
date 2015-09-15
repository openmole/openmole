package org.openmole.gui.misc.js

//import fr.iscpif.scaladget.api.ClassKeyAggregator
import org.scalajs.dom.raw.{ HTMLButtonElement, HTMLElement, HTMLSpanElement }
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import bs._

/*
 * Copyright (C) 02/09/15 // mathieu.leclaire@openmole.org
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

object OMTags {

  implicit def omClassKeyAggregatorToScladgetClassKeyAggregator(ck: ClassKeyAggregator): fr.iscpif.scaladget.api.ClassKeyAggregator =
    fr.iscpif.scaladget.api.BootstrapTags.key(ck.key)

  def waitingSpan(text: String, buttonCB: ClassKeyAggregator): TypedTag[HTMLSpanElement] =
    bs.span("btn " + buttonCB.key)(
      bs.span("loading")(text)
    )

  def glyphBorderButton(text: String,
                        buttonCB: ClassKeyAggregator,
                        glyCA: ClassKeyAggregator, todo: () ⇒ Unit): TypedTag[HTMLButtonElement] = {
    tags.button(`type` := "button", `class` := "btn " + buttonCB.key, onclick := { () ⇒ todo() })(
      tags.span(aria.hidden := true)(glyph(glyCA))
    )
  }

  val glyph_plug = "icon-power-cord"
  val glyph_book = "icon-book"
}
