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

import fr.in2p3.jsaga.adaptor.job.SubState
import fr.in2p3.jsaga.adaptor.job.local.LocalJobProcess
import fr.in2p3.jsaga.adaptor.job.monitor.JobStatus

object SSHJobStatus {
  val PROCESS_CANCELED = 143
  
  def apply(jobId: String, retCode: Int) = 
    new SSHJobStatus(jobId,
      if(retCode >= LocalJobProcess.PROCESS_DONE_OK)
            if(retCode == PROCESS_CANCELED) SubState.CANCELED
            else SubState.DONE
      else SubState.FAILED_ERROR)
         
  def running(jobId: String) = new SSHJobStatus(jobId, SubState.RUNNING_ACTIVE)
  
}

class SSHJobStatus private (
  jobId: String,
  state: SubState,
  retCode: Int = 0) extends JobStatus(jobId, null, state.toSagaState.name, retCode) {
  
  override def getModel = "ssh"

  override def getSubState = state
}
