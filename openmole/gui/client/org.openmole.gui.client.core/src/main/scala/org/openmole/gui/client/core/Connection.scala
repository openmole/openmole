package org.openmole.gui.client.core

import scaladget.bootstrapnative.bsn._

import org.openmole.gui.ext.tool.client._
import org.scalajs.dom.raw.HTMLFormElement

import scalatags.JsDom.all._
import scalatags.JsDom.tags

/*
 * Copyright (C) 07/11/16 // mathieu.leclaire@openmole.org
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

object Connection {

  lazy val connectButton = tags.button("Connect", btn_primary, `type` := "submit").render

  val passwordInput = inputTag("")(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    marginBottom := 15,
    name := "password",
    autofocus := true
  ).render

  def cleanInputs = {
    passwordInput.value = ""
  }

  private val connectionForm: HTMLFormElement =
    tags.form(
      method := "post",
      passwordInput,
      connectButton
    ).render

  val render = {
    div(
      SettingsView.renderConnection,
      div(omsheet.connectionTabOverlay)(
        div(
          img(src := "img/openmole.png", omsheet.openmoleLogo),
          div(
            omsheet.centerPage(),
            div(
              omsheet.connectionBlock,
              connectionForm
            )
          )
        )
      )
    ).render

  }
}
