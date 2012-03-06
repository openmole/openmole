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

package org.openmole.plugin.environment.ssh

import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.Workspace
import fr.in2p3.jsaga.impl.context.ContextImpl
import org.openmole.core.batch.environment.Authentication
import org.openmole.core.batch.jsaga.JSAGASessionService

object SSHAuthentication {
  
  def apply(login: String, host: String) = {
    val list = Workspace.persistentList(classOf[SSHAuthenticationMethod])
    val connection = login + '@' + host
    
    new SSHAuthentication(
      list.find{case(i, e) => connection.matches(e.target)}.getOrElse(throw new UserBadDataError("No authentication method found for " + connection))._2
    )
  }
  
}


class SSHAuthentication(val method: SSHAuthenticationMethod) extends Authentication {
  
  override def key = method.target

  override def initialize = {
    val ctxSSH = method.context
    ctxSSH.setVectorAttribute(ContextImpl.BASE_URL_INCLUDES, Array("ssh->ssh2://*"))
    JSAGASessionService.addContext("ssh://" + method.target, ctxSSH)
    
    val ctxSFTP = method.context
    ctxSFTP.setVectorAttribute(ContextImpl.BASE_URL_INCLUDES, Array("sftp->sftp2://*"))
    JSAGASessionService.addContext("sftp://" + method.target, ctxSFTP)
  }
  
}
