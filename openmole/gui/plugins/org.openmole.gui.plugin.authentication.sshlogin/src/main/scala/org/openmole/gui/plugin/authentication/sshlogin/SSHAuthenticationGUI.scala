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
import boopickle.Default._
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import autowire._

import org.scalajs.dom.raw.HTMLElement
import org.openmole.gui.ext.data._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

@JSExportTopLevel("org.openmole.gui.plugin.authentication.sshlogin.LoginAuthenticationFactory")
class LoginAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = LoginAuthenticationData

  def buildEmpty: AuthenticationPlugin = new LoginAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new LoginAuthenticationGUI(data)

  def name = "SSH Login/Password"

  def getData: Future[Seq[AuthType]] = OMPost()[LoginAuthenticationAPI].loginAuthentications().call()
}

@JSExportTopLevel("org.openmole.gui.plugin.authentication.sshlogin.LoginAuthenticationGUI")
class LoginAuthenticationGUI(val data: LoginAuthenticationData = LoginAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = LoginAuthenticationData

  def factory = new LoginAuthenticationFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[LoginAuthenticationAPI].removeAuthentication(data).call().foreach { _ ⇒
    onremove()
  }

  val loginInput = input(data.login)(placeholder := "Login").render

  val passwordInput = input(data.password)(placeholder := "Password", passwordType).render

  val targetInput = input(data.target)(placeholder := "Host").render

  val portInput = input(data.port)(placeholder := "Port").render

  def panel: TypedTag[HTMLElement] = hForm(
    loginInput.withLabel("Login"),
    passwordInput.withLabel("Password"),
    targetInput.withLabel("Target"),
    portInput.withLabel("Port")
  )

  def save(onsave: () ⇒ Unit): Unit = {
    OMPost()[LoginAuthenticationAPI].removeAuthentication(data).call().foreach { d ⇒
      OMPost()[LoginAuthenticationAPI].addAuthentication(LoginAuthenticationData(loginInput.value, passwordInput.value, targetInput.value, portInput.value)).call().foreach { b ⇒
        onsave()
      }
    }
  }

  def test: Future[Seq[Test]] = OMPost()[LoginAuthenticationAPI].testAuthentication(data).call()
}