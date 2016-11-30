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

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.client.tool.OMPost
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.HTMLFormElement
import rx.Rx
import sheet._

import scalatags.JsDom.all._
import scalatags.JsDom.tags
import org.openmole.gui.client.tool._
import JsRxTags._

class ResetPassword {
  val shutDown = new ShutDown

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

  lazy val resetButton = bs.button("Set password", btn_primary, () ⇒ {
    OMPost()[Api].resetPassword().call()
    ()
  }).render

  def setPasswordForm: HTMLFormElement =
    tags.form(
      passwordInput,
      passwordAgainInput,
      resetButton
    ).render

  val resetPassDiv = div(
    shutDown.shutdownButton,
    Rx {
      div(omsheet.connectionTabOverlay)(
        div(
          img(src := "img/openmole.png", omsheet.openmoleLogo),
          div(
            omsheet.centerPage,
            div(
              if (shutDown.alert.now)
                shutDown.alertPanel
              else {
                div(
                  omsheet.connectionBlock,
                  setPasswordForm
                )
              }
            )
          )
        )
      )
    }
  )
}
