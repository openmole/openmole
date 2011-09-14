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

import fr.in2p3.jsaga.adaptor.job.local.LocalJobProcess

object SSHJobProcess {
  val rootDir = ".jsaga/var/adaptor/ssh"
  
  def getFile(jobId: String, suffix: String) = rootDir + "/" + jobId + "." + suffix
  def getPidFile(jobId: String) = getFile(jobId, "pid")
  def getEndCodeFile(jobId: String) = getFile(jobId, "end")
  
}

class SSHJobProcess(jobId: String) extends LocalJobProcess(jobId) {

  import SSHJobProcess._

  override def getReturnCode = m_returnCode

  
  override def getFile(suffix: String) = rootDir + "/" + m_jobId + "." + suffix
  
  def getJobDir =  rootDir + "/" + m_jobId
  def getPidFile = SSHJobProcess.getPidFile(jobId)
  def getEndCodeFile = SSHJobProcess.getEndCodeFile(jobId)
  
}
