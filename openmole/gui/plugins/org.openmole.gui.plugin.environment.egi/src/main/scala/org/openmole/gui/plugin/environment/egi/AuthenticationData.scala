package org.openmole.gui.plugin.environment.egi

import org.openmole.gui.ext.data.{ AuthenticationData, Test, FailedTest, Error, ErrorBuilder }

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

// Those 3 classes do not derive from Test because of bug: https://github.com/lihaoyi/upickle-pprint/issues/97
object AuthenticationTest {
  def passed = AuthenticationTestImpl("", true, Error.empty)

  def pending = AuthenticationTestImpl("pending", false, Error.empty)

  def error(msg: String, passed: Boolean, err: Error) = AuthenticationTestImpl(msg, passed, err)
}

sealed trait AuthenticationTest {
  def message: String

  def passed: Boolean

  def errorStack: Error
}

case class AuthenticationTestImpl(message: String, passed: Boolean, errorStack: Error) extends AuthenticationTest

/*object PasswordAuthenticationTest {
  def passed = PasswordAuthenticationTest("", true, Error.empty)
  def pending = PasswordAuthenticationTest("pending", false, Error.empty)
}

case class ProxyAuthenticationTest(message: String, passed: Boolean, errorStack: Error) extends AuthenticationTest
object ProxyAuthenticationTest {
  def passed =  ProxyAuthenticationTest("", true, Error.empty)
  def pending = ProxyAuthenticationTest("pending", false, Error.empty)
}

case class DiracAuthenticationTest(message: String, passed: Boolean, errorStack: Error) extends AuthenticationTest
object DiracAuthenticationTest {
  def passed = DiracAuthenticationTest("", true, Error.empty)
  def pending = DiracAuthenticationTest("pending", false, Error.empty)
}*/

case class EGIAuthenticationTest(
  message:  String,
  password: AuthenticationTest = AuthenticationTest.pending,
  proxy:    AuthenticationTest = AuthenticationTest.pending,
  dirac:    AuthenticationTest = AuthenticationTest.pending
) extends Test {
  def errorStack = Error(s"${password.errorStack.stackTrace} \n\n ${proxy.errorStack.stackTrace} \n\n ${dirac.errorStack.stackTrace}")

  def passed = password.passed && proxy.passed && dirac.passed
}

case class SSHAuthenticationTest(passed: Boolean, errorStack: Error) extends Test {
  def message: String = if (passed) "OK" else "failed"
}
