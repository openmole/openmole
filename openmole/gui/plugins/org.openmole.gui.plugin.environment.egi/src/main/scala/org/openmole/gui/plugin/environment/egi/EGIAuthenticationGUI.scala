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
package org.openmole.gui.plugin.environment.egi

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.ext.data.{ AuthenticationPlugin, AuthenticationPluginFactory }
import org.openmole.gui.ext.tool.client.OMPost
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import autowire._
import sheet._
import bs._
import org.openmole.core.workspace.Decrypt
import org.openmole.core.workspace.Workspace
import scala.concurrent.Future
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
class EGIAuthenticationGUIFactory extends AuthenticationPluginFactory {
  type AuthType = EGIAuthenticationData

  def buildEmpty: AuthenticationPlugin = new EGIAuthenticationGUI

  def build(data: AuthType): AuthenticationPlugin = new EGIAuthenticationGUI(data)

  def name = "EGI P12 certificate"

  def getData: Future[Seq[AuthType]] = OMPost()[API].egiAuthentications().call()
}

@JSExport
class EGIAuthenticationGUI(val data: EGIAuthenticationData = EGIAuthenticationData()) extends AuthenticationPlugin {
  type AuthType = EGIAuthenticationData

  val passwordStyle: ModifierSeq = Seq(
    width := 130,
    passwordType
  )

  val password = bs.input(data.cypheredPassword)(placeholder := "Password", passwordStyle).render
  val privateKey = FileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined, Some("egi.p12"))

  def factory = new EGIAuthenticationGUIFactory

  def remove(onremove: () ⇒ Unit) = OMPost()[API].removeAuthentication().call().foreach { _ ⇒
    onremove()
  }

  @JSExport
  def panel() =
    hForm(
      password.withLabel("Password"),
      privateKey.view.render
    )

  def save(onsave: () ⇒ Unit) = {
    OMPost()[API].removeAuthentication().call().foreach { d ⇒
      OMPost()[API].addAuthentication(EGIAuthenticationData(
        cypheredPassword = password.value,
        privateKey = if (privateKey.pathSet.now) Some("egi.p12") else None
      )).call().foreach { b ⇒
        onsave()
      }
    }
  }
}