/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.authentication

import org.ogf.saga.context.Context
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace

object HostAuthenticationMethod {

  def apply(login: String, host: String) = {
    val list = Workspace.persistentList(classOf[HostAuthenticationMethod])
    val connection = login + '@' + host

    list.find { case (i, e) ⇒ connection.matches(e.regexp) }.getOrElse(throw new UserBadDataError("No authentication method found for " + connection))._2
  }

}

trait HostAuthenticationMethod extends AuthenticationMethod {
  def target: String
  def regexp = ".*" + target + ".*"
  def context: Context

  def init = JSAGASessionService.addContext(regexp, context)

  def method = classOf[HostAuthenticationMethod]
  override def toString = "Target = " + target
}
