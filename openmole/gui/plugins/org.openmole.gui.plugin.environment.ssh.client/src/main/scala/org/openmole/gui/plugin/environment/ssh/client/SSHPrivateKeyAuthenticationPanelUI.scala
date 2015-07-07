package org.openmole.gui.plugin.environment.ssh.client

import java.net.URI

import org.openmole.gui.client.core.{Settings, OMPost}
import org.openmole.gui.client.core.files.AuthFileUploaderUI
import org.openmole.gui.ext.data.{FileExtension, SafePath, PrivateKeyAuthenticationData}
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.misc.utils.Utils
import org.openmole.gui.shared.Api
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.{BootstrapTags => bs}
import scalatags.JsDom.{tags ⇒ tags}

/*
 * Copyright (C) 01/07/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@JSExport("org.openmole.gui.plugin.environment.ssh.client.SSHPrivateKeyAuthenticationPanelUI")
class SSHPrivateKeyAuthenticationPanelUI(data: PrivateKeyAuthenticationData) extends PanelUI {


  val login = bs.input(data.login)(
    placeholder := "Login",
    width := "130px").render

  val target = bs.input(data.target)(
    placeholder := "Host",
    width := "130px").render

  val password = bs.input(data.cypheredPassword)(
    placeholder := "Password",
    `type` := "password",
    width := "130px").render

  lazy val privateKey = new AuthFileUploaderUI(data.privateKey.map{_.leaf}.getOrElse(""), data.privateKey.isDefined)

  @JSExport
  val view = {
    tags.div(
      bs.labeledField("Login", login),
      bs.labeledField("Host", target),
      bs.labeledField("Password", password),
      bs.labeledField("Key file", privateKey.view)
    )
  }

  def save(onsave: () => Unit) =
    OMPost[Api].removeAuthentication(data).call().foreach { d ⇒
      Settings.authenticationKeysPath.foreach { kp =>
        OMPost[Api].addAuthentication(
          PrivateKeyAuthenticationData(Some(kp / SafePath.leaf(privateKey.fileName, FileExtension.NO_EXTENSION)),
            login.value,
            password.value,
            target.value)).call().foreach { b =>
          onsave()
        }
      }
    }

}