package org.openmole.gui.client.core.authentications

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.client.core.files.AuthFileUploaderUI
import org.openmole.gui.ext.data.PrivateKeyAuthenticationData
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.shared.Api
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.{ tags ⇒ tags }
import AuthenticationUtils._

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

class SSHPrivateKeyAuthenticationPanel(data: PrivateKeyAuthenticationData) extends PanelUI {

  val login = loginInput(data.login)
  val target = targetInput(data.target)
  val port = portInput(data.port)
  val password = passwordInput(data.cypheredPassword)

  lazy val privateKey = new AuthFileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined)

  val view = {
    tags.div(
      bs.labeledField("Login", login),
      bs.labeledField("Password", password),
      bs.labeledField("Host", target),
      bs.labeledField("Port", port),
      bs.labeledField("Key file", privateKey.view)
    )
  }

  def save(onsave: () ⇒ Unit) =
    OMPost[Api].removeAuthentication(data).call().foreach { d ⇒
      OMPost[Api].addAuthentication(
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