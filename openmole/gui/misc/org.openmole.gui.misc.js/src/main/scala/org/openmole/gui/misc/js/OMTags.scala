package org.openmole.gui.misc.js

import fr.iscpif.scaladget.api.ClassKeyAggregator
import org.scalajs.dom.html._
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLButtonElement, HTMLElement, HTMLSpanElement }
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.JsRxTags._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import bs._
import rx._

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

  // TO PORT IN SCALADGET
  val glyph_upload_alt = "glyphicon-upload"
  val glyph_arrow_right = "glyphicon-arrow-right"
  val glyph_arrow_left = "glyphicon-arrow-left"
  val glyph_arrow_right_and_left = "glyphicon-resize-horizontal"
  val glyph_filter = "glyphicon-filter"
  val glyph_copy = "glyphicon-copy"
  val glyph_paste = "glyphicon-paste"

  def buttonGroup(keys: ClassKeyAggregator = emptyCK) = bs.div("btn-group " + keys.key)

  def alert(alertType: ClassKeyAggregator, content: TypedTag[HTMLDivElement], todook: () ⇒ Unit, todocancel: () ⇒ Unit, buttonGroupClass: ClassKeyAggregator = "left") =
    tags.div(role := "alert")(
      content,
      bs.div("spacer20")(
        buttonGroup(buttonGroupClass)(
          bs.button("OK", btn_danger, todook),
          bs.button("Cancel", btn_default, todocancel)
        )
      )
    )

  def glyphString(glyph: String) = "glyphicon " + glyph

  def glyphSpan(glyCA: ClassKeyAggregator, linkName: String = "")(todo: ⇒ Unit): TypedTag[HTMLSpanElement] =
    tags.span(cursor := "pointer", glyph(glyCA)(linkName)(onclick := { () ⇒
      todo
    }))

  def glyphSpan(rxString: Rx[String])(todo: ⇒ Unit): TypedTag[HTMLSpanElement] =
    tags.span(cursor := "pointer", `class` := rxString, onclick := { () ⇒
      todo
    })

  private def cbSpan(name: String) = tags.span(name, style := "position: relative; margin-right:5px; margin-left:5px; top: -3px;")

  def checkbox(name: String, default: Boolean)(todo: Input ⇒ Unit) = {
    lazy val cb: Input = tags.input(`type` := "checkbox", checked := default.toString, onclick := { () ⇒ todo(cb) }).render
    tags.div(
      cbSpan(name),
      cb
    )
  }

  def checkbox(default: Boolean, name: String = "")(todo: Input ⇒ Unit) = {
    lazy val cb: Input = tags.input(`type` := "checkbox", checked := default.toString, onclick := { () ⇒ todo(cb) }).render
    tags.div(
      cb,
      cbSpan(name)
    )
  }
}
