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
import org.openmole.gui.client.ext.*
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
  @JSExportTopLevel("authentication_sshlogin")
  val sshlogin = js.Object {
    given LoginAuthenticationServerAPI()
    new org.openmole.gui.plugin.authentication.sshlogin.LoginAuthenticationFactory
  }
}

class LoginAuthenticationFactory(using api: LoginAuthenticationAPI) extends AuthenticationPluginFactory:
  type AuthType = LoginAuthenticationData
  def buildEmpty: AuthenticationPlugin = new LoginAuthenticationGUI
  def build(data: AuthType): AuthenticationPlugin = new LoginAuthenticationGUI(data)
  def name = "SSH Login/Password"
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]] = api.loginAuthentications()

class LoginAuthenticationGUI(val data: LoginAuthenticationData = LoginAuthenticationData())(using api: LoginAuthenticationAPI) extends AuthenticationPlugin:
  type AuthType = LoginAuthenticationData
  def factory = new LoginAuthenticationFactory

  def remove(using basePath: BasePath, notificationAPI: NotificationService) = api.removeAuthentication(data)

  val loginInput = inputTag(data.login).amend(placeholder := "Login")
  val passwordInput = inputTag(data.password).amend(placeholder := "Password", `type` := "password")
  val targetInput = inputTag(data.target).amend(placeholder := "Host")
  val portInput = inputTag(data.port).amend(placeholder := "Port")

  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): HtmlElement = div(
    flexColumn, width := "400px", height := "220",
    div(cls := "verticalFormItem", div("Login", width:="150px"), loginInput),
    div(cls := "verticalFormItem", div("Password", width:="150px"), passwordInput),
    div(cls := "verticalFormItem", div("Target", width:="150px"), targetInput),
    div(cls := "verticalFormItem", div("Port", width:="150px"), portInput)
  )

  def save(using basePath: BasePath, notificationAPI: NotificationService) =
    for
      _ <- remove
      _ <- api.addAuthentication(LoginAuthenticationData(loginInput.ref.value, passwordInput.ref.value, targetInput.ref.value, portInput.ref.value))
    yield ()

  def test(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] = api.testAuthentication(data)
