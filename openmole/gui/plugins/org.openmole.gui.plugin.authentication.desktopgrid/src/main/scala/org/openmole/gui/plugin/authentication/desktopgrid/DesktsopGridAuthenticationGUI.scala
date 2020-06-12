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
package org.openmole.gui.plugin.authentication.desktopgrid

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.OMPost
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import autowire._
import sheet._
import bs._
import org.scalajs.dom.raw.HTMLElement
import org.openmole.gui.ext.data._

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

@JSExportTopLevel("DesktopGridAuthenticationFactory")
class DesktopGridAuthenticationFactory extends AuthenticationPluginFactory {
  type AuthType = DesktopGridAuthenticationData

  def buildEmpty: AuthenticationPlugin = new DesktopGridAuthenticationGUI()

  def build(data: AuthType): AuthenticationPlugin = new DesktopGridAuthenticationGUI(data)

  def name = "Desktop grid"

  def getData: Future[Seq[AuthType]] = OMPost()[DesktopGridAuthenticationAPI].desktopGridAuthentications().call()
}

@JSExportTopLevel("DesktopGridAuthenticationGUI")
class DesktopGridAuthenticationGUI(val data: DesktopGridAuthenticationData = DesktopGridAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = DesktopGridAuthenticationData

  def factory = new DesktopGridAuthenticationFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[DesktopGridAuthenticationAPI].removeAuthentication().call().foreach { _ ⇒
    onremove()
  }

  val passwordInput = bs.inputTag(data.password)(placeholder := "Password", passwordType).render

  def panel: TypedTag[HTMLElement] = hForm(
    passwordInput.withLabel("Password")
  )

  def save(onsave: () ⇒ Unit): Unit = {
    OMPost()[DesktopGridAuthenticationAPI].updateAuthentication(DesktopGridAuthenticationData(passwordInput.value)).call().foreach { b ⇒
      onsave()
    }
  }

  def test: Future[Seq[Test]] = Future(Seq(Test.passed()))
}