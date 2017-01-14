package org.openmole.gui.plugin.environment.egi

import org.openmole.gui.ext.data.Error

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

sealed trait AuthenticationData {
  def name: String
}

case class EGIAuthenticationData(
    cypheredPassword: String         = "",
    privateKey:       Option[String] = None
) extends AuthenticationData {
  def name = "egi.p12"
}

case class LoginPasswordAuthenticationData(
    login:            String = "",
    cypheredPassword: String = "",
    target:           String = "",
    port:             String = "22"
) extends AuthenticationData {
  def name = s"$login@$target"
}

case class PrivateKeyAuthenticationData(
    privateKey:       Option[String] = None,
    login:            String         = "",
    cypheredPassword: String         = "",
    target:           String         = "",
    port:             String         = "22"
) extends AuthenticationData {
  def name = s"$login@$target"
}

object AuthenticationTest {
  def empty = AuthenticationTestBase(false, Error(""))
}

sealed trait AuthenticationTest {
  def passed: Boolean

  def errorStack: Error
}

case class AuthenticationTestBase(passed: Boolean, errorStack: Error) extends AuthenticationTest

case class EGIAuthenticationTest(message: String, password: AuthenticationTest, proxy: AuthenticationTest, dirac: AuthenticationTest) extends AuthenticationTest {
  def errorStack = Error(s"${password.errorStack.stackTrace} \n\n ${proxy.errorStack.stackTrace} \n\n ${dirac.errorStack.stackTrace}")

  def passed = password.passed && proxy.passed && dirac.passed
}

case class SSHAuthenticationTest(passed: Boolean, errorStack: Error) extends AuthenticationTest {
  def message: String = if (passed) "OK" else "failed"
}
