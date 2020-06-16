package org.openmole.gui.plugin.authentication.egi

import org.openmole.gui.ext.data
import org.openmole.gui.ext.data.{ AuthenticationData, ErrorData, MessageErrorData, Test }

/*
 * Copyright (C) 12/01/17 // mathieu.leclaire@openmole.org
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

object EGIAuthenticationData {
  def authenticationDirectory = "${AUTHENTICATION_DIRECTORY}"
}

case class EGIAuthenticationData(
  cypheredPassword: String         = "",
  privateKey:       Option[String] = None
) extends AuthenticationData {
  def name = "egi.p12"
}

object EGIAuthenticationTest {
  def apply(
    message:  String,
    password: Test   = Test.pending,
    proxy:    Test   = Test.pending,
    dirac:    Test   = Test.pending
  ): Test = {
    val all = Seq(password, proxy, dirac)
    val error = all.flatMap(_.error).headOption

    error match {
      case Some(e) ⇒ Test.error("failed", e)
      case None if all.exists { t ⇒ t == Test.pending } ⇒ Test.pending
      case _ ⇒ Test.passed(message)
    }
  }
}