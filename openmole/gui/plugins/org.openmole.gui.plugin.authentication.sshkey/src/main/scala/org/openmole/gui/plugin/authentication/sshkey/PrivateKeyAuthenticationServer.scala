package org.openmole.gui.plugin.authentication.sshkey

/*
 * Copyright (C) 2022 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



import org.openmole.core.services.Services
import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.gui.server.ext.utils
import org.openmole.plugin.environment.ssh.*
import org.openmole.tool.file.*

import util.{Failure, Success, Try}
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.shared.data.ServerFileSystemContext.Authentication

class PrivateKeyAuthenticationServer(s: Services)
  extends APIServer with PrivateKeyAuthenticationAPI:

  val privateKeyAuthenticationsRoute =
    privateKeyAuthentications.errorImplementedBy { _ => impl.privateKeyAuthentications() }

  val addAuthenticationRoute =
    addAuthentication.errorImplementedBy { a => impl.addAuthentication(a) }

  val removeAuthenticationRoute =
    removeAuthentication.errorImplementedBy { impl.removeAuthentication }

  val testAuthenticationRoute =
    testAuthentication.errorImplementedBy { a => impl.testAuthentication(a) }

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(privateKeyAuthenticationsRoute, addAuthenticationRoute, removeAuthenticationRoute, testAuthenticationRoute)
  )

  object impl:
    import s._

    private def coreObject(data: PrivateKeyAuthenticationData) =
      data.privateKeyPath.map: pk =>
        PrivateKey(
          safePathToFile(pk),
          data.login,
          data.password,
          data.target,
          data.port.toInt
        )

    def privateKeyAuthentications(): Seq[PrivateKeyAuthenticationData] =
      SSHAuthentication().flatMap:
        case key: PrivateKey ⇒
          val safePath = key.privateKey.toSafePath(using ServerFileSystemContext.Authentication)
          Seq:
            PrivateKeyAuthenticationData(
              privateKey = Some(safePath.name),
              login = key.login,
              password = key.password,
              target = key.host,
              port = key.port.toString,
              directory = safePath.parent
            )
        case _ ⇒ None


    def addAuthentication(data: PrivateKeyAuthenticationData): Unit =
      coreObject(data).foreach: co ⇒
        SSHAuthentication += co

    def removeAuthentication(data: PrivateKeyAuthenticationData, deleteKey: Boolean): Unit =
      coreObject(data).foreach: co ⇒
        SSHAuthentication -= co
      if deleteKey && data.directory.parent == SafePath.root(ServerFileSystemContext.Authentication) 
      then safePathToFile(data.directory).recursiveDelete

    def testAuthentication(data: PrivateKeyAuthenticationData): Seq[Test] =
      val sshKey = coreObject(data)

      sshKey match
        case None => Seq()
        case Some(value) =>
          def testConnection =
            SSHAuthentication.test(value).failed.toOption.map: f =>
              Test.error("Connection failed", ErrorData(f))

          def testPassword =
            testSSHKeyPassword(value.privateKey.getAbsolutePath, value.password).map: f =>
              Test.error("Password is incorrect", ErrorData(f))

          testPassword orElse testConnection  match
            case None => Seq(Test.passed())
            case Some(t) => Seq(t)

