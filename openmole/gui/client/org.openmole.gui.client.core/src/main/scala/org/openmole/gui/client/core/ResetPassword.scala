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
import org.openmole.gui.client.ext._
import com.raquo.laminar.api.L._

class ResetPassword {

  val passwordInput = inputTag("").amend(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    marginBottom := "15",
    //name := "password",
    onMountFocus
  )

  val passwordAgainInput =
    inputTag("").amend(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      nameAttr := "passwordagain",
    )

  lazy val resetButton = button("Set password", btn_primary, marginTop := "15", `type` := "submit")

  def setPasswordForm =
    form(
      method := "post",
      cls := "connection-form",
      passwordInput,
      passwordAgainInput,
      resetButton
    )

  val resetPassDiv =
    div(
      cls := "screen-center",
      img(src := "img/openmole_light.png", width := "600px"),
      setPasswordForm
    )

}
