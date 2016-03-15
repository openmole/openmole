package org.openmole.gui.server.core

/*
 * Copyright (C) 24/09/14 // mathieu.leclaire@openmole.org
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

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.server.core.Utils._
import org.openmole.plugin.environment.egi.{ EGIAuthentication, P12Certificate }
import org.openmole.plugin.environment.ssh.{ PrivateKey, SSHAuthentication, LoginPassword }

object AuthenticationFactories {

  implicit def dataToFactory(data: AuthenticationData): AuthenticationFactory = data match {
    case e: EGIP12AuthenticationData        ⇒ EGIP12Factory
    case l: LoginPasswordAuthenticationData ⇒ SSHLoginPasswordFactory
    case _                                  ⇒ SSHPrivateKeyFactory

  }
  def addAuthentication(data: AuthenticationData) = data.buildAuthentication(data)
  def allAuthentications(data: AuthenticationData) = data.allAuthenticationData
  def allAuthentications = EGIP12Factory.allAuthenticationData ++ SSHLoginPasswordFactory.allAuthenticationData ++ SSHPrivateKeyFactory.allAuthenticationData
  def removeAuthentication(data: AuthenticationData) = data.removeAuthentication(data)

  trait AuthenticationFactory {
    def coreObject(data: AuthenticationData): Option[Any]

    def buildAuthentication(data: AuthenticationData): Unit

    def allAuthenticationData: Seq[AuthenticationData]

    def removeAuthentication(data: AuthenticationData): Unit
  }

  object EGIP12Factory extends AuthenticationFactory {
    implicit def authProvider = Workspace.authenticationProvider

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.foreach { a ⇒
        EGIAuthentication.update(a)
      }
    }

    def allAuthenticationData: Seq[AuthenticationData] = {
      EGIAuthentication() match {
        case Some(p12: P12Certificate) ⇒
          Seq(EGIP12AuthenticationData(
            Workspace.decrypt(p12.cypheredPassword),
            Some(p12.certificate.getName)
          ))
        case x: Any ⇒ Seq()
      }
    }

    def coreObject(data: AuthenticationData): Option[P12Certificate] = data match {
      case p12: EGIP12AuthenticationData ⇒
        p12.privateKey match {
          case Some(pk: String) ⇒ Some(P12Certificate(
            Workspace.encrypt(p12.cypheredPassword),
            authenticationFile(pk)
          ))
          case _ ⇒ None
        }
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = EGIAuthentication.clear
  }

  object SSHLoginPasswordFactory extends AuthenticationFactory {

    implicit def authProvider = Workspace.authenticationProvider

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.map { a ⇒ SSHAuthentication += a }
    }

    def allAuthenticationData: Seq[AuthenticationData] = SSHAuthentication().flatMap {
      _ match {
        case lp: LoginPassword ⇒ Some(LoginPasswordAuthenticationData(
          lp.login,
          Workspace.decrypt(lp.cypheredPassword),
          lp.host //FIXME SUPPORT port lp.port
        ))
        case _ ⇒ None
      }
    }

    def coreObject(data: AuthenticationData): Option[LoginPassword] = data match {
      case lp: LoginPasswordAuthenticationData ⇒ Some(LoginPassword(
        lp.login,
        Workspace.encrypt(lp.cypheredPassword),
        lp.target
      ))
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = coreObject(data).map { e ⇒
      SSHAuthentication -= e
    }

  }

  object SSHPrivateKeyFactory extends AuthenticationFactory {
    implicit def authProvider = Workspace.authenticationProvider

    def buildAuthentication(data: AuthenticationData) = {
      val auth = coreObject(data)
      auth.map { a ⇒ SSHAuthentication += a }
    }

    def allAuthenticationData: Seq[AuthenticationData] = SSHAuthentication().flatMap {
      _ match {
        case key: PrivateKey ⇒ Some(PrivateKeyAuthenticationData(
          Some(key.privateKey.getName),
          key.login,
          Workspace.decrypt(key.cypheredPassword),
          key.host //FIXME SUPPORT port key.port
        ))
        case _ ⇒ None
      }
    }

    def coreObject(data: AuthenticationData): Option[PrivateKey] = data match {
      case keyData: PrivateKeyAuthenticationData ⇒
        keyData.privateKey match {
          case Some(pk: String) ⇒ Some(PrivateKey(
            authenticationFile(pk),
            keyData.login,
            Workspace.encrypt(keyData.cypheredPassword),
            keyData.target
          ))
          case _ ⇒ None
        }
      case _ ⇒ None
    }

    def removeAuthentication(data: AuthenticationData) = coreObject(data).map { e ⇒ SSHAuthentication -= e }

  }

}

