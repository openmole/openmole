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

package org.openmole.plugin.environment.ssh

import org.openmole.core.batch.environment._
import java.net.URI
import org.openmole.misc.workspace.{ Workspace, ConfigurationLocation }

object SSHService {
  val timeout = new ConfigurationLocation("SSH", "TimeOut")
  Workspace += (timeout, "PT120S")

}

trait SSHService extends fr.iscpif.gridscale.ssh.SSHHost with BatchService {
  def authentication: fr.iscpif.gridscale.ssh.SSHAuthentication
  val url = new URI("ssh", null, host, port, null, null, null)
}
