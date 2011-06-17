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

package org.openmole.runtime.daemon

import org.openmole.core.batch.file.URIFile
import org.openmole.core.batch.jsaga.SSHAuthentication

class JobLaucher {
  def launch = {
    val auth = new SSHAuthentication("localhost", 22, "reuillon", "tete088")
    auth.initialize
    new URIFile("sftp://localhost/iscpif/users/reuillon").list.foreach(println)
  }
}
