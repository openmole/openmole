/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.batch.jsaga

import org.ogf.saga.context.Context
import org.openmole.core.batch.environment.Authentication

class SSHAuthentication(host: String, port:Int, login: String, password: String) extends Authentication{
  
  override def key = "ssh:" + (host, port, login).toString
  
  override def expires = Long.MaxValue

  override def initialize = {
    val ctx = JSAGASessionService.createContext
    ctx.setAttribute(Context.TYPE, "SSH")
    ctx.setAttribute(Context.REMOTEHOST, host)
    ctx.setAttribute(Context.REMOTEPORT, port.toString)
    ctx.setAttribute(Context.USERID, login)
    ctx.setAttribute(Context.USERPASS, password)
    JSAGASessionService.addContext(ctx)
  }
  
}
