/*
 * Copyright (C) 2011 Romain Reuillon
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

class LoginPassword(
    val login: String,
    val cypheredPassword: String,
    val target: String) extends HostAuthenticationMethod with CypheredPassword {

  override def context = {
    val ctx = JSAGASessionService.createContext
    ctx.setAttribute(Context.TYPE, "UserPass")
    ctx.setAttribute(Context.USERID, login)
    ctx.setAttribute(Context.USERPASS, password)
    ctx
  }

  override def toString = super.toString + ", Login / password, login = " + login

}
