package org.openmole.plugin.environment.miniclust

/*
 * Copyright (C) 2025 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import io.circe.*
import io.circe.generic.auto.*

object MiniclustAuthentication:

  def eq(a1: MiniclustAuthentication, a2: MiniclustAuthentication) =
    (a1, a2) match
      case (a1: LoginPassword, a2: LoginPassword) => (a1.login, a1.url) == (a2.login, a2.url)

  def apply()(using store: AuthenticationStore, cypher: Cypher): Seq[LoginPassword] =
    Authentication.load[MiniclustAuthentication].map:
      case l: LoginPassword => l.copy(password = cypher.decrypt(l.password))

  def +=(a: MiniclustAuthentication)(using store: AuthenticationStore, cypher: Cypher): Unit =
    val s =
      a match
        case l: LoginPassword => l.copy(password = cypher.encrypt(l.password))

    Authentication.save[MiniclustAuthentication](s, eq)

  def -=(a: MiniclustAuthentication)(using AuthenticationStore): Unit =
    Authentication.remove[MiniclustAuthentication](a, eq)

  def find(login: String, url: String)(using AuthenticationStore, Cypher): MiniclustAuthentication =
    val list = apply()
    val auth = list.findLast(a => (a.login, a.url) == (login, url))
    auth.getOrElse(throw new UserBadDataError(s"No authentication method found for $login@$url"))

  def test(a: MiniclustAuthentication) =
    import scala.util.*
    Try:
      MiniclustEnvironment.toMiniclust(a, insecure = true)
    .map(_ => true)


  case class LoginPassword(url: String, login: String, password: String) extends MiniclustAuthentication:
    override def toString = s"$login@$url using password"

sealed trait MiniclustAuthentication


