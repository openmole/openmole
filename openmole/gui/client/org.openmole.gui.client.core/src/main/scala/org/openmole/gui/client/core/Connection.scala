package org.openmole.gui.client.core

import java.net.URI

import fr.iscpif.scaladget.stylesheet.all._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ HTMLElement, HTMLFormElement }
import rx.{ Rx, Var }

import scalatags.JsDom.all._
import scalatags.JsDom.tags
import org.openmole.gui.misc.js.JsRxTags._
import org.apache.http.client.methods._
import org.apache.http.impl.client.HttpClientBuilder

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

class Connection {

  val shutDown = new ShutDown
  val passwordChosen = Var(true)

  OMPost[Api].passwordState().call().foreach { b ⇒
    passwordChosen() = b.chosen
  }

  lazy val connectButton = tags.button("Connect", btn_primary, `type` := "submit").render

  val passwordInput = bs.input("")(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    name := "password",
    autofocus := true
  ).render

  val passwordAgainInput =
    bs.input("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      name := "passwordagain",
      autofocus,
      display := Rx {
        if (passwordChosen()) "none" else "inline"
      }
    ).render

  def cleanInputs = {
    passwordInput.value = ""
    passwordAgainInput.value = ""
  }

  def resetPassword = OMPost[Api].resetPassword().call().foreach { b ⇒
    passwordChosen() = false
    cleanInputs
  }


  def connectionForm: HTMLFormElement =
    tags.form(
      method := "post",
      passwordInput,
      passwordAgainInput,
      connectButton
    ).render

  val connectionDiv = div(
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
                  connectionForm
                )
              }
            )
          )
        )
      )
    }
  )

}
