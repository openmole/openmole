package org.openmole.gui.client.core.authentications

import org.openmole.gui.client.core.files.AuthFileUploaderUI
import org.openmole.gui.ext.data.{ PanelUI, PrivateKeyAuthenticationData }

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import autowire._

import scalatags.JsDom.all._
import AuthenticationUtils._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.tool.client.OMPost

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

class SSHPrivateKeyAuthenticationPanel(data: PrivateKeyAuthenticationData = PrivateKeyAuthenticationData()) extends PanelUI {

  val login = loginInput(data.login)
  val target = targetInput(data.target)
  val port = portInput(data.port)
  val password = passwordInput(data.cypheredPassword)
  lazy val privateKey = new AuthFileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined)

  val view = form(sheet.formInline)(
    for {
      e ← Seq(login, password, target, port)
    } yield {
      e.render
    },
    privateKey.view
  )

  def save(onsave: () ⇒ Unit) =
    OMPost()[Api].removeAuthentication(data).call().foreach { d ⇒
      OMPost()[Api].addAuthentication(
        PrivateKeyAuthenticationData(
          Some(privateKey.fileName),
          login.value,
          password.value,
          target.value,
          port.value
        )
      ).call().foreach { b ⇒
          onsave()
        }
    }

}