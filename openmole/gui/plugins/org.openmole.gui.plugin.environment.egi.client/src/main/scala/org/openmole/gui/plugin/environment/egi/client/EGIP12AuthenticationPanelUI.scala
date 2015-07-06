package org.openmole.gui.plugin.environment.egi.client

import org.openmole.gui.client.core.{Settings, OMPost}
import org.openmole.gui.client.core.files.AuthFileUploaderUI
import org.openmole.gui.ext.data.{FileExtension, SafePath, EGIP12AuthenticationData}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.shared.Api

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{BootstrapTags => bs}
import scalatags.JsDom.{tags ⇒ tags}
import org.openmole.gui.server.core.Utils._

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

@JSExport("org.openmole.gui.plugin.environment.egi.client.EGIP12AuthenticationPanelUI")
class EGIP12AuthenticationPanelUI(data: EGIP12AuthenticationData) extends PanelUI {

  val password = bs.input(data.cypheredPassword)(
    placeholder := "Password",
    `type` := "password",
    width := "130px").render


  lazy val privateKey = new AuthFileUploaderUI(data.certificatePath.leaf, Some("egi.p12"))

  @JSExport
  val view = tags.div(
    bs.labeledField("Key file", privateKey.view),
    bs.labeledField("Password", password)
  )


  def save(onsave: () => Unit) = Settings.authenticationKeysPath.foreach { kp => {
    OMPost[Api].addAuthentication(EGIP12AuthenticationData(password.value,
      kp / SafePath.leaf("egi.p12", FileExtension.NO_EXTENSION))).call().foreach { b =>
      onsave()
    }
  }
  }

}