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

import fr.in2p3.jsaga.impl.context.ContextImpl
import org.openmole.core.batch.authentication._

object SSHAuthentication {

  def apply(login: String, host: String) =
    new SSHAuthentication(HostAuthenticationMethod(login, host))

}

class SSHAuthentication(val method: HostAuthenticationMethod) extends Authentication {

  override def key = method.target

  override def initialize(local: Boolean) = {
    val ctxSSH = method.context
    ctxSSH.setVectorAttribute(ContextImpl.BASE_URL_INCLUDES, Array("ssh->ssh2://*"))
    ctxSSH.setVectorAttribute(ContextImpl.BASE_URL_INCLUDES, Array("sftp->sftp2://*"))
    JSAGASessionService.addContext(method.target, ctxSSH)
  }

}
