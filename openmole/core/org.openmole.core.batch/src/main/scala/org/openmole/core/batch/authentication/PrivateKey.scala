/*
 * Copyright (C) 2012 reuillon
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

class PrivateKey(
    val privateKeyPath: String,
    val publicKeyPath: String,
    val login: String,
    val cypheredPassword: String,
    val target: String) extends HostAuthenticationMethod with CypheredPassword {

  def this(login: String, cypheredPassword: String, keyType: String, target: String) =
    this(
      System.getProperty("user.home") + "/.ssh/id_" + keyType,
      System.getProperty("user.home") + "/.ssh/id_" + keyType + ".pub",
      login,
      cypheredPassword,
      target)

  def this(login: String, keyType: String, target: String) = this(login, "", keyType, target)

  override def context = {
    val ctx = JSAGASessionService.createContext
    ctx.setAttribute(Context.TYPE, "SSH")
    ctx.setAttribute(Context.USERID, login)
    ctx.setAttribute(Context.USERCERT, publicKeyPath)
    ctx.setAttribute(Context.USERKEY, privateKeyPath)
    ctx.setAttribute(Context.USERPASS, password)
    ctx
  }

  override def toString =
    super.toString + ", " + "PublicKey = " + publicKeyPath +
      ", PrivateKey = " + privateKeyPath +
      ", Login = " + login

}
