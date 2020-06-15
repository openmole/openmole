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
import boopickle.Default._
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.{ FileUploaderUI, OMPost }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.all._

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

  def getData: Future[Seq[AuthType]] = OMPost()[PrivateKeyAuthenticationAPI].privateKeyAuthentications().call()
}

class PrivateKeyAuthenticationGUI(val data: PrivateKeyAuthenticationData = PrivateKeyAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = PrivateKeyAuthenticationData

  val passwordStyle: ModifierSeq = Seq(
    width := 130,
    passwordType
  )
  val privateKey = new FileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined)

  val loginInput = inputTag(data.login)(placeholder := "Login").render

  val passwordInput = inputTag(data.cypheredPassword)(placeholder := "Password", passwordType).render

  val targetInput = inputTag(data.target)(placeholder := "Host").render

  val portInput = inputTag(data.port)(placeholder := "Port").render

  def factory = new PrivateKeyAuthenticationFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[PrivateKeyAuthenticationAPI].removeAuthentication(data).call().foreach { _ ⇒
    onremove()
  }

  lazy val panel = vForm(
    hForm(
      loginInput.withLabel("Login"),
      passwordInput.withLabel("Password"),
      targetInput.withLabel("Host"),
      portInput.withLabel("Port")
    ).render,
    privateKey.view(marginTop := 10).render.withLabel("Private key")
  )

  def save(onsave: () ⇒ Unit) = {
    OMPost()[PrivateKeyAuthenticationAPI].removeAuthentication(data).call().foreach {
      d ⇒
        OMPost()[PrivateKeyAuthenticationAPI].addAuthentication(PrivateKeyAuthenticationData(
          privateKey = Some(privateKey.fileName),
          loginInput.value,
          passwordInput.value,
          targetInput.value,
          portInput.value
        )).call().foreach {
          b ⇒
            onsave()
        }
    }
  }

  def test = OMPost()[PrivateKeyAuthenticationAPI].testAuthentication(data).call()

}
