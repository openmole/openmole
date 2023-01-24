/**
 * Created by Romain Reuillon on 28/11/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.gui.plugin.authentication.sshlogin

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.{flexColumn, flexRow}
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.scalajs.dom.raw.HTMLElement
import org.openmole.gui.shared.data.*

import scala.concurrent.Future
import scala.scalajs.js.annotation.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*

import scalajs.js

object TopLevelExports {
  @JSExportTopLevel("sshlogin")
  val sshlogin = js.Object {
    new org.openmole.gui.plugin.authentication.sshlogin.LoginAuthenticationFactory
  }
}

class LoginAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = LoginAuthenticationData

  def buildEmpty: AuthenticationPlugin = new LoginAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new LoginAuthenticationGUI(data)

  def name = "SSH Login/Password"

  def getData: Future[Seq[AuthType]] = PluginFetch.future(_.loginAuthentications(()).future)
}

class LoginAuthenticationGUI(val data: LoginAuthenticationData = LoginAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = LoginAuthenticationData

  def factory = new LoginAuthenticationFactory

  def remove(onremove: () ⇒ Unit) = PluginFetch.future(_.removeAuthentication(data).future).foreach { _ ⇒
    onremove()
  }

  val loginInput = inputTag(data.login).amend(placeholder := "Login")

  val passwordInput = inputTag(data.password).amend(placeholder := "Password", `type` := "password")

  val targetInput = inputTag(data.target).amend(placeholder := "Host")

  val portInput = inputTag(data.port).amend(placeholder := "Port")

  def panel(using api: ServerAPI): HtmlElement = div(
    flexColumn, width := "400px", height := "220",
    div(cls := "verticalFormItem", div("Login", width:="150px"), loginInput),
    div(cls := "verticalFormItem", div("Password", width:="150px"), passwordInput),
    div(cls := "verticalFormItem", div("Target", width:="150px"), targetInput),
    div(cls := "verticalFormItem", div("Port", width:="150px"), portInput)
  )

  def save(onsave: () ⇒ Unit): Unit = {
    PluginFetch.future(_.removeAuthentication(data).future).foreach { d ⇒
      PluginFetch.future(_.addAuthentication(LoginAuthenticationData(loginInput.ref.value, passwordInput.ref.value, targetInput.ref.value, portInput.ref.value)).future).foreach { b ⇒
        onsave()
      }
    }
  }

  def test: Future[Seq[Test]] = PluginFetch.future(_.testAuthentication(data).future)
}