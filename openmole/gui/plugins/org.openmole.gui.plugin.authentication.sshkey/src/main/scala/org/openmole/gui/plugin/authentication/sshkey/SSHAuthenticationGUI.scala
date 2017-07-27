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

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.{ FileUploaderUI, OMPost }
import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import sheet._
import bs._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.all._

@JSExportTopLevel("org.openmole.gui.plugin.authentication.sshkey.PrivateKeyAuthenticationFactory")
class PrivateKeyAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = PrivateKeyAuthenticationData

  def buildEmpty: AuthenticationPlugin = new PrivateKeyAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new PrivateKeyAuthenticationGUI(data)

  def name = "SSH Private key"

  def getData: Future[Seq[AuthType]] = OMPost()[PrivateKeyAuthenticationAPI].privateKeyAuthentications().call()
}

@JSExportTopLevel("org.openmole.gui.plugin.authentication.sshkey.PrivateKeyAuthenticationGUI")
class PrivateKeyAuthenticationGUI(val data: PrivateKeyAuthenticationData = PrivateKeyAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = PrivateKeyAuthenticationData

  val passwordStyle: ModifierSeq = Seq(
    width := 130,
    passwordType
  )
  val privateKey = new FileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined)

  val loginInput = bs.input(data.login)(placeholder := "Login").render

  val passwordInput = bs.input(data.cypheredPassword)(placeholder := "Password", passwordType).render

  val targetInput = bs.input(data.target)(placeholder := "Host").render

  val portInput = bs.input(data.port)(placeholder := "Port").render

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
    privateKey.view(sheet.marginTop(10)).render.withLabel("Private key")
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
