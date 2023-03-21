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
package org.openmole.gui.plugin.authentication.sshkey

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.Future
import scala.scalajs.js.annotation.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*

import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("sshkey")
  val sshkey = js.Object {
    new org.openmole.gui.plugin.authentication.sshkey.PrivateKeyAuthenticationFactory
  }
}

class PrivateKeyAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = PrivateKeyAuthenticationData
  def buildEmpty: AuthenticationPlugin = new PrivateKeyAuthenticationGUI
  def build(data: AuthType): AuthenticationPlugin = new PrivateKeyAuthenticationGUI(data)
  def name = "SSH Private key"
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]] = PluginFetch.futureError(_.privateKeyAuthentications(()).future)
}

class PrivateKeyAuthenticationGUI(val data: PrivateKeyAuthenticationData = PrivateKeyAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = PrivateKeyAuthenticationData

  val passwordStyle: HESetters = Seq(
    width := "130",
    `type` := "password"
  )
  val privateKeyUploader = FileUploaderUI(data.privateKey)

  val loginInput = inputTag(data.login).amend(placeholder := "Login")

  val passwordInput = inputTag(data.password).amend(placeholder := "Password", `type` := "password")

  val targetInput = inputTag(data.target).amend(placeholder := "Host")

  val portInput = inputTag(data.port).amend(placeholder := "Port")

  def factory = new PrivateKeyAuthenticationFactory

  def remove(using basePath: BasePath, notificationAPI: NotificationService) = PluginFetch.futureError(_.removeAuthentication(data).future)

  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) = div(
    flexColumn, width := "400px", height := "220",
    div(cls := "verticalFormItem", div("Login", width := "150px"), loginInput),
    div(cls := "verticalFormItem", div("Password", width := "150px"), passwordInput),
    div(cls := "verticalFormItem", div("Target", width := "150px"), targetInput),
    div(cls := "verticalFormItem", div("Port", width := "150px"), portInput),
    div(cls := "verticalFormItem", div("Private key", width := "150px"), display.flex, div(privateKeyUploader.view.amend(flexRow, justifyContent.flexEnd), width := "100%"))
  )


  def save(using basePath: BasePath, notificationAPI: NotificationService) =
    for
      - <- remove
      _ <-
        PluginFetch.futureError(_.addAuthentication(PrivateKeyAuthenticationData(
          privateKey = privateKeyUploader.file.now(),
          loginInput.ref.value,
          passwordInput.ref.value,
          targetInput.ref.value,
          portInput.ref.value)).future)
    yield ()

  def test(using basePath: BasePath, notificationAPI: NotificationService) = PluginFetch.futureError(_.testAuthentication(data).future)

}
