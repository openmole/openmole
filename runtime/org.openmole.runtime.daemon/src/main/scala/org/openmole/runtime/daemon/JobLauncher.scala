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

import java.net.URI
import java.net.URL
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.batch.file.URIFile
import org.openmole.plugin.environment.desktop.SFTPAuthentication
import org.openmole.plugin.environment.desktop.DesktopEnvironment._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object JobLauncher {
  val jobCheckInterval = new ConfigurationLocation("JobLauncher", "jobCheckInterval") 
  Workspace += (jobCheckInterval, "PT1M")
}

class JobLauncher {
  import JobLauncher._
  
  def launch(userHostPort: String, password: String) = {
    val splitUser = userHostPort.split("@") 
    if(splitUser.size != 2) throw new UserBadDataError("Host must be formated as user@hostname")
    val user = splitUser(0)
    val splitHost = splitUser(1).split(":")
    val port = if(splitHost.size == 2) splitHost(1).toInt else 22
    val host = splitHost(0)
   
    val auth = new SFTPAuthentication(host, port, user, password)
    auth.initialize
    
    while(true) {
      try{
        new URIFile("sftp://" + host + ":" + port + "/" + jobsDirName + '/').list.foreach(println)
      } catch {
        case e: Exception => Logger.getLogger(this.getClass.getName).log(Level.WARNING, "Error while looking for jobs.",e)
      }
      Thread.sleep(Workspace.preferenceAsDurationInMs(jobCheckInterval))
    }
  }
}
