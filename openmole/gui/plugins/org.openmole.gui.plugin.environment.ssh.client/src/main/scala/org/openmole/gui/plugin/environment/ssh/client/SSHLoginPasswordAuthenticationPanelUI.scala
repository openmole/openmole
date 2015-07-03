package org.openmole.gui.plugin.environment.ssh.client

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.ext.data.LoginPasswordAuthenticationData
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.shared.Api

import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.{BootstrapTags => bs}
import scalatags.JsDom.{tags ⇒ tags}

/*
 * Copyright (C) 16/06/15 // mathieu.leclaire@openmole.org
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

@JSExport("org.openmole.gui.plugin.environment.ssh.client.SSHLoginPasswordAuthenticationPanelUI")
class SSHLoginPasswordAuthenticationPanelUI(data: LoginPasswordAuthenticationData) extends PanelUI {

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



  @JSExport
  val view = {
    tags.div(
      bs.labeledRow("Login", login),
      bs.labeledRow("Host", target),
      bs.labeledRow("Password", password)
    )
  }

  def save(onsave: ()=> Unit) = {
    OMPost[Api].addAuthentication(LoginPasswordAuthenticationData(login.value, password.value, target.value)).call().foreach{b=>
      onsave()
    }
  }

}