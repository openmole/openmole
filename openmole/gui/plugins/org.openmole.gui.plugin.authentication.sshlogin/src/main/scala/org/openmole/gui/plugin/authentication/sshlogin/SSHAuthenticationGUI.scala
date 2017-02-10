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

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.OMPost
import org.openmole.gui.ext.tool.client.JsRxTags._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import sheet._
import bs._
import org.scalajs.dom.raw.HTMLElement
import org.openmole.gui.ext.data._
import scala.concurrent.Future
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

@JSExport
class LoginAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = LoginAuthenticationData

  def buildEmpty: AuthenticationPlugin = new LoginAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new LoginAuthenticationGUI(data)

  def name = "SSH Login/Password"

  def getData: Future[Seq[AuthType]] = OMPost()[LoginAuthenticationAPI].loginAuthentications().call()
}

@JSExport
class LoginAuthenticationGUI(val data: LoginAuthenticationData = LoginAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = LoginAuthenticationData

  def factory = new LoginAuthenticationFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[LoginAuthenticationAPI].removeAuthentication(data).call().foreach { _ ⇒
    onremove()
  }

  val loginInput = bs.input(data.login)(placeholder := "Login").render

  val passwordInput = bs.input(data.cypheredPassword)(placeholder := "Password", passwordType).render

  val targetInput = bs.input(data.target)(placeholder := "Host").render

  val portInput = bs.input(data.port)(placeholder := "Port").render

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

//@JSExport
//class EGIAuthenticationGUI(val data: EGIAuthenticationData = EGIAuthenticationData()) extends AuthenticationPlugin {
//  type AuthType = EGIAuthenticationData
//  type TestType = EGIAuthenticationTest
//
//  val passwordStyle: ModifierSeq = Seq(
//    width := 130,
//    passwordType
//  )
//
//  val password = bs.input(data.cypheredPassword)(placeholder := "Password", passwordStyle).render
//  val privateKey = FileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined, Some("egi.p12"))
//
//  val voInput = bs.input("")(placeholder := "vo1,vo2").render
//
//  OMPost()[Api].getConfigurationValue(VOTest).call().foreach {
//    _.foreach { c ⇒
//      voInput.value = c
//    }
//  }
//
//  def factory = new EGIAuthenticationGUIFactory
//
//  def remove(onremove: () ⇒ Unit) = OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach { _ ⇒
//    onremove()
//  }
//
//  @JSExport
//  lazy val panel = vForm(
//    password.withLabel("Password"),
//    privateKey.view(sheet.marginTop(10)).render,
//    voInput.withLabel("Test EGI credential on", sheet.paddingTop(40))
//  )
//
//  def save(onsave: () ⇒ Unit) = {
//    OMPost()[EGIAuthenticationAPI].removeAuthentication().call().foreach {
//      d ⇒
//        OMPost()[EGIAuthenticationAPI].addAuthentication(EGIAuthenticationData(
//          cypheredPassword = password.value,
//          privateKey = if (privateKey.pathSet.now) Some("egi.p12") else None
//        )).call().foreach {
//          b ⇒
//            onsave()
//        }
//    }
//
//    OMPost()[Api].setConfigurationValue(VOTest, voInput.value).call()
//  }
//
//  def test = {
//    OMPost()[EGIAuthenticationAPI].testAuthentication(data).call()
//  }
//}