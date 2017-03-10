package org.openmole.gui.client.core

/*
 * Copyright (C) 15/11/16 // mathieu.leclaire@openmole.org
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

import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }

import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.scalajs.dom.raw.HTMLFormElement
import rx.{ Ctx, Rx }
import sheet._
import org.openmole.gui.ext.tool.client._
import scalatags.JsDom.all._
import scalatags.JsDom.tags
import org.openmole.gui.ext.tool.client.JsRxTags._

class ResetPassword {

  val passwordInput = bs.input("")(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    sheet.marginBottom(15),
    name := "password",
    autofocus := true
  ).render

  val passwordAgainInput =
    bs.input("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      name := "passwordagain",
      autofocus
    ).render

  lazy val resetButton = tags.button("Set password", btn_primary +++ sheet.marginTop(15), `type` := "submit").render

  def setPasswordForm: HTMLFormElement =
    tags.form(
      action := "/resetPassword",
      method := "post",
      passwordInput,
      passwordAgainInput,
      resetButton
    ).render

  val resetPassDiv =
    div(omsheet.connectionTabOverlay)(
      div(
        img(src := "img/openmole.png", omsheet.openmoleLogo),
        div(
          omsheet.centerPage,
          div(
            omsheet.connectionBlock,
            setPasswordForm
          )
        )
      )
    )
}
