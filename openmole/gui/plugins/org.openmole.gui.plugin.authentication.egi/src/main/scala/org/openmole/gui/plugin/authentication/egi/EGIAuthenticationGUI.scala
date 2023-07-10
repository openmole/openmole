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
package org.openmole.gui.plugin.authentication.egi

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*

import scala.concurrent.Future
import scala.scalajs.js.annotation.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.{SafePath, ServerFileSystemContext}
import scaladget.bootstrapnative.bsn

import scala.scalajs.js

object TopLevelExports {
  @JSExportTopLevel("authentication_egi")
  val egi = js.Object {
    new org.openmole.gui.plugin.authentication.egi.EGIAuthenticationGUIFactory
  }
}

class EGIAuthenticationGUIFactory extends AuthenticationPluginFactory:
  type AuthType = EGIAuthenticationData
  def buildEmpty: AuthenticationPlugin = new EGIAuthenticationGUI
  def build(data: AuthType): AuthenticationPlugin = new EGIAuthenticationGUI(data)
  def name = "EGI"
  def getData(using basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AuthType]] =
    PluginFetch.futureError(_.egiAuthentications(()).future)



class EGIAuthenticationGUI(val data: EGIAuthenticationData = EGIAuthenticationData()) extends AuthenticationPlugin:
  type AuthType = EGIAuthenticationData

  val password = inputTag(data.password).amend(placeholder := "Password", `type` := "password")

  val certificateUpload = FileUploaderUI(data.privateKey, Some(SafePath(Seq("egi.p12"), ServerFileSystemContext.Authentication)))

  val voInputContent = Var[String]("")
  val voInput = inputTag("").amend(placeholder := "vo1,vo2", onInput.mapToValue --> voInputContent)
  //val voInput = input(bsn.formControl, value := voInputContent).amend(placeholder := "vo1,vo2")


  def factory = new EGIAuthenticationGUIFactory

  def remove(using basePath: BasePath, notificationAPI: NotificationService) = PluginFetch.futureError(_.removeAuthentications(()).future)

  def panel(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService) = {
    import scaladget.tools._
      //      _.foreach { c ⇒ a.voInput.ref.value = c }
      //    }
    div(
      flexColumn, width := "400px", height := "220",
      div(cls := "verticalFormItem", div("Password", width := "150px"), password),
      div(cls := "verticalFormItem", div("Certificate", width := "150px"), display.flex, div(certificateUpload.view.amend(flexRow, justifyContent.flexEnd), width := "100%")),
      div(cls := "verticalFormItem", div("Test EGI credential on", width := "150px"), voInput),
      EventStream.fromFuture(PluginFetch.futureError(_.getVOTests(()).future), true) --> Observer[Seq[String]] { v => voInput.ref.value = v.mkString(",") }
    )

  }

  def save(using basePath: BasePath, notificationAPI: NotificationService) =
    def vos =
      voInputContent.now() match
        case "" => Seq()
        case s => s.trim.split(',').map(_.trim).toSeq

    for
      _ <- remove
      _ <- PluginFetch.futureError(_.addAuthentication(EGIAuthenticationData(password = password.ref.value, privateKey = certificateUpload.file.now())).future)
      _ <- PluginFetch.futureError(_.setVOTests(vos).future)
    yield ()

  def test(using basePath: BasePath, notificationAPI: NotificationService) = PluginFetch.futureError(_.testAuthentication(data).future)


