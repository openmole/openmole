package org.openmole.gui.client.tool

import org.scalajs.dom.raw._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

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

  def waitingSpan(text: String, button: ModifierSeq): TypedTag[HTMLSpanElement] =
    span(button)(
      span("loading")(text)
    )

  def glyphBorderButton(
    text:     String,
    buttonCB: ModifierSeq,
    glyCA:    ModifierSeq, todo: () ⇒ Unit
  ): TypedTag[HTMLButtonElement] = {
    tags.button(`type` := "button", buttonCB, onclick := { () ⇒ todo() })(
      tags.span(aria.hidden := true)(glyCA)
    )
  }

  val glyph_plug = toClass("glyphicon icon-power-cord")
  val glyph_book = toClass("glyphicon icon-book")
  val glyph_data = toClass("glyphicon icon-database")
  val glyph_share = toClass("glyphicon glyphicon-share-alt")
  val options = toClass("glyphicon glyphicon-option-horizontal")
  val glyph_eye_open = toClass("glyphicon glyphicon-eye-open")

  case class AlertAction(action: () ⇒ Unit)

  def alert(alertType: ModifierSeq, content: TypedTag[HTMLDivElement], actions: Seq[AlertAction], buttonGroupClass: ModifierSeq = floatLeft +++ (marginLeft := 20), okString: String = "OK"): TypedTag[HTMLDivElement] =
    actions.size match {
      case 1 ⇒ alert(alertType, content, actions.head.action, buttonGroupClass, okString)
      case 2 ⇒ alert(alertType, content, actions.head.action, actions(1).action, buttonGroupClass, okString)
      case _ ⇒ tags.div()
    }

  def alert(alertType: ModifierSeq, content: TypedTag[HTMLDivElement], todook: () ⇒ Unit, buttonGroupClass: ModifierSeq, okString: String): TypedTag[HTMLDivElement] =
    tags.div(role := "alert")(
      content,
      button(okString, alertType +++ (paddingTop := 20), onclick := todook)
    )

  def alert(alertType: ModifierSeq, content: TypedTag[HTMLDivElement], todook: () ⇒ Unit, todocancel: () ⇒ Unit, buttonGroupClass: ModifierSeq, okString: String): TypedTag[HTMLDivElement] =
    tags.div(role := "alert", overflowY := "scroll", height := 600, padding := 20)(
      content,
      div(paddingTop := 20)(
        buttonGroup(buttonGroupClass)(
          button(okString, alertType, onclick := todook),
          button("Cancel", btn_default, onclick := todocancel)
        )
      )
    )

  def glyphSpan(glyCA: ModifierSeq, linkName: String = "", todo: ⇒ Unit = () ⇒ {}): TypedTag[HTMLSpanElement] =
    tags.span(cursor := "pointer", glyCA)(linkName)(onclick := { () ⇒
      todo
    })

  def uploadButton(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] = {
    span(ms("btn-file"), cursor := "pointer", id := "success-like")(
      glyphSpan(glyph_upload),
      fileInputMultiple(todo)
    )
  }
}
