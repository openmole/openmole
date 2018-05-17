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

import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import org.scalajs.dom.raw.HTMLFormElement
import org.openmole.gui.ext.tool.client._
import scalatags.JsDom.all._
import scalatags.JsDom.tags
class ResetPassword {

  val passwordInput = inputTag("")(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    marginBottom := 15,
    name := "password",
    autofocus := true
  ).render

  val passwordAgainInput =
    inputTag("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      name := "passwordagain",
      autofocus
    ).render

  lazy val resetButton = tags.button("Set password", btn_primary +++ (marginTop := 15), `type` := "submit").render

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
          omsheet.centerPage(),
          div(
            omsheet.connectionBlock,
            setPasswordForm
          )
        )
      )
    )
}
