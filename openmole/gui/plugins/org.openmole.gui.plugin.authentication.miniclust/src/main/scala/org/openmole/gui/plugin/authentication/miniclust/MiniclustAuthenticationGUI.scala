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
package org.openmole.gui.plugin.authentication.miniclust

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import org.scalajs.dom.raw.HTMLElement
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object TopLevelExports:
  @JSExportTopLevel("authentication_miniclust")
  val sshlogin = js.Object:
    given MiniclustAuthenticationServerAPI()
    new org.openmole.gui.plugin.authentication.miniclust.MiniclustAuthenticationFactory

class MiniclustAuthenticationFactory(using api: MiniclustAuthenticationAPI) extends AuthenticationPluginFactory:
  type AuthType = MiniclustAuthenticationData
  def buildEmpty = new MiniclustAuthenticationGUI
  def build(data: AuthType) = new MiniclustAuthenticationGUI(data)
  def name = "MiniClust Login/Password"
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]] = api.loginAuthentications()
  def remove(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService) = api.removeAuthentication(data)
  def test(data: AuthType)(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[Test]] = api.testAuthentication(data)

class MiniclustAuthenticationGUI(val data: MiniclustAuthenticationData = MiniclustAuthenticationData())(using api: MiniclustAuthenticationAPI) extends AuthenticationPlugin[MiniclustAuthenticationData]:
  def name = s"${data.login}@${data.url}"
  
  val loginInput = inputTag(data.login).amend(placeholder := "Login")
  val passwordInput = inputTag(data.password).amend(placeholder := "Password", `type` := "password")
  val urlInput = inputTag(data.url).amend(placeholder := "https://miniclust.domain")

  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): HtmlElement = div(
    flexColumn, width := "400px", height := "220",
    div(cls := "verticalFormItem", div("Login", width:="150px"), loginInput),
    div(cls := "verticalFormItem", div("Password", width:="150px"), passwordInput),
    div(cls := "verticalFormItem", div("URL", width:="150px"), urlInput),
  )

  def save(using basePath: BasePath, notificationAPI: NotificationService) =
    for
      _ <- api.removeAuthentication(data)
      _ <- api.addAuthentication(MiniclustAuthenticationData(loginInput.ref.value, passwordInput.ref.value, urlInput.ref.value))
    yield ()

