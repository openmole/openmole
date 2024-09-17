package org.openmole.gui.client.tool

import org.scalajs.dom.raw._
import scaladget.bootstrapnative.bsn._
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L._
import com.raquo.laminar.codecs.*

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

object OMTags:

  def waitingSpan(text: String, button: HESetters): HtmlElement =
    span(
      button,
      span(s"loading $text")
    )

  def glyphBorderButton(
    text:     String,
    buttonCB: HESetters,
    glyCA:    HESetters,
    todo:     () ⇒ Unit
  ): HtmlElement = {
    button(
      `type` := "button",
      buttonCB,
      onClick --> { _ ⇒ todo() },
      span(aria.hidden := true, glyCA)
    )
  }

  val glyph_plug = cls("bi-plug-fill")
  val glyph_data = cls("bi-server")
  val glyph_clock = cls("bi-clock")
  val glyph_share = cls("bi-forward-fill")
  //val options = cls("glyphicon glyphicon-option-horizontal")
  val glyph_eye_open = cls("bi-eye-fill")
  val glyph_flash = cls("bi-lightning-charge-fill")
  val glyph_file = cls("bi-file")
  val glyph_house = cls("bi-house-door-fill")
  val glyph_arrow_left_right = cls("bi-arrow-left-right")
  val glyph_extract = cls("bi-file-earmark-zip-fill")
  lazy val glyph_filter = cls("bi-funnel-fill")
  lazy val glyph_search = cls("bi-search")
  val glyph_puzzle = cls("bi-puzzle-fill")
  val glyph_unpuzzle = cls("bi-clock")
  val glyph_play = cls("bi-file-play")
  val glyph_gear = cls("bi-gear-fill")
  val glyph_info = cls("bi-info-circle-fill")
  val buttonOM = cls("btn btnOm")
  val btn_purple = cls("btn btn-purple")
  val down = cls("bi bi-arrow-down-square")
  val glyph_copy = cls("bi-copy")
  val glyph_move = cls("bi-box-arrow-right")

  case class AlertAction(action: () ⇒ Unit)

  def alert(alertType: HESetters, content: HtmlElement, actions: Seq[AlertAction], buttonGroupClass: HESetters = Seq(float := "left", marginLeft := "20"), okString: String = "OK", cancelString: String = "Cancel"): HtmlElement =
    actions.size match {
      case 1 ⇒ alert(alertType, content, actions.head.action, buttonGroupClass, okString)
      case 2 ⇒ alert(alertType, content, actions.head.action, actions(1).action, buttonGroupClass, okString, cancelString)
      case _ ⇒ div()
    }

  def alert(alertType: HESetters, content: HtmlElement, todook: () ⇒ Unit, buttonGroupClass: HESetters, okString: String): HtmlElement =
    div(
      role := "alert",
      content,
      button(okString, alertType, paddingTop := "20", onClick --> (_ ⇒ todook()))
    )

  def alert(alertType: HESetters, content: HtmlElement, todook: () ⇒ Unit, todocancel: () ⇒ Unit, buttonGroupClass: HESetters, okString: String, cancelString: String): HtmlElement =
    div(role := "alert", overflowY := "scroll", height := "600", padding := "20",
      content,
      div(
        paddingTop := "20",
        buttonGroup.amend(
          buttonGroupClass,
          button(okString, alertType, onClick --> (_ ⇒ todook())),
          button(cancelString, btn_secondary, onClick --> (_ ⇒ todocancel()))
        )
      )
    )

  def glyphSpan(glyCA: HESetters, linkName: String = "", todo: ⇒ Unit = () ⇒ {}): HtmlElement =
    span(cursor := "pointer", glyCA, linkName, onClick --> (_ ⇒ todo))

  def uploadButton(todo: Input ⇒ Unit): HtmlElement = {
    span(
      cls := "btn-file",
      cursor.pointer,
      idAttr := "success-like",
      glyphSpan(glyph_upload),
      fileInput(todo).amend(multiple := true)
    )
  }

  val webkitdirectory = new HtmlAttr[Boolean]("webkitdirectory", BooleanAsAttrPresenceCodec)
  val mozdirectory = new HtmlAttr[Boolean]("mozdirectory", BooleanAsAttrPresenceCodec)

  def omFileInput(todo: Input => Unit, directory: Boolean = false) =
    fileInput(todo).amend(L.multiple := true,
      if directory
      then Seq(
        webkitdirectory := true,
        mozdirectory := true)
      else emptyMod
    )
