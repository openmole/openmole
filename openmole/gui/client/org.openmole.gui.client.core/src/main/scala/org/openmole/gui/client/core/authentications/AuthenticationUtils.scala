package org.openmole.gui.client.core.authentications

import org.openmole.gui.ext.data.{ EGIP12AuthenticationData, AuthenticationData }
import org.scalajs.dom.html.{ Input, Label }

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import sheet._

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

  val passwordStyle: ModifierSeq = Seq(
    width := 130,
    passwordType
  )

  val basicStyle = width := 130
  val portStyle = width := 130

  def loginInput(default: String) = bs.labeledInput("Login", default, "Login", inputStyle = basicStyle)

  def targetInput(default: String) = bs.labeledInput("Host", default, "Host", inputStyle = basicStyle)

  def portInput(default: String) = bs.labeledInput("Port", default, "Port", inputStyle = portStyle)

  def passwordInput(default: String) = bs.labeledInput("Password", default, "Password", inputStyle = passwordStyle)

}
