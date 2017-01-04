package org.openmole.gui.client.core.authentications

import org.openmole.gui.client.core.files.AuthFileUploaderUI

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.data.{ EGIP12AuthenticationData, PanelUI }
import AuthenticationUtils._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._
import org.openmole.gui.ext.api.Api

/*
 * Copyright (C) 02/07/15 // mathieu.leclaire@openmole.org
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

class EGIP12AuthenticationPanel(data: EGIP12AuthenticationData = EGIP12AuthenticationData()) extends PanelUI {

  val password = passwordInput(data.cypheredPassword)
  val privateKey = new AuthFileUploaderUI(data.privateKey.getOrElse(""), data.privateKey.isDefined, Some("egi.p12"))

  val view = hForm(
    password.withLabel("Password"),
    privateKey.view.render
  )

  def save(onsave: () ⇒ Unit) =
    org.openmole.gui.client.core.post()[Api].removeAuthentication(data).call().foreach { d ⇒
      org.openmole.gui.client.core.post()[Api].addAuthentication(EGIP12AuthenticationData(
        password.value,
        if (privateKey.pathSet.now) Some("egi.p12") else None
      )).call().foreach { b ⇒
        onsave()
      }
    }

}