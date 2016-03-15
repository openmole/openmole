package org.openmole.gui.client.core.authentications

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._

/*
 * Copyright (C) 15/03/16 // mathieu.leclaire@openmole.org
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

object AuthenticationUtils {

  def defaultInput(default: String, pHolder: String, w: String) = bs.input(default, key("spacer5"))(
    placeholder := pHolder,
    width := w
  ).render

  def loginInput(default: String) = defaultInput(default, "Login", "130px")

  def targetInput(default: String) = defaultInput(default, "Host", "130px")

  def portInput(default: String) = defaultInput(default, "Port", "60px")

  def passwordInput(default: String) = bs.input(default, key("spacer5"))(
    placeholder := "Password",
    `type` := "password",
    width := "130px"
  ).render

}
