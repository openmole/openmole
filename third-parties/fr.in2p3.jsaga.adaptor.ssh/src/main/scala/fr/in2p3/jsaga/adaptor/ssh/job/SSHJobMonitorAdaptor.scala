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

package fr.in2p3.jsaga.adaptor.ssh.job

import ch.ethz.ssh2.SFTPv3Client
import fr.in2p3.jsaga.adaptor.job.control.manage.ListableJobAdaptor
import fr.in2p3.jsaga.adaptor.job.monitor.QueryIndividualJob
import fr.in2p3.jsaga.adaptor.ssh.SSHAdaptor
import fr.in2p3.jsaga.adaptor.ssh.data.SFTPDataAdaptor
import java.io.ByteArrayOutputStream
import org.ogf.saga.error.NoSuccessException

class SSHJobMonitorAdaptor extends SSHAdaptor with QueryIndividualJob {

  override def getType = "ssh"
    
  override def  getStatus(jobId: String) = 
    try  
  {
    val sftpClient = new SFTPv3Client(connection)
    try {
      val stream = new ByteArrayOutputStream
      SFTPDataAdaptor.getToStream(sftpClient, SSHJobProcess.getEndCodeFile(jobId), "", stream)
      SSHJobStatus(jobId, new String(stream.toByteArray).toInt)
    } catch {
      case e => 
        if(SFTPDataAdaptor.exists(sftpClient, SSHJobProcess.getPidFile(jobId), "")) SSHJobStatus.running(jobId)
        else throw e
    } finally sftpClient.close
  } catch {
    case e => throw new NoSuccessException(e)
  }
  
}
