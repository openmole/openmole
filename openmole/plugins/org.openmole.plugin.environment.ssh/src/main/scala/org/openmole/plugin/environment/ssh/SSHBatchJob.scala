///*
// * Copyright (C) 2011 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.environment.ssh
//
//import fr.iscpif.gridscale.ssh.SSHJobDescription
//import org.openmole.core.workflow.execution.ExecutionState._
//import org.openmole.plugin.environment.batch.control._
//import org.openmole.plugin.environment.batch.jobservice.BatchJob
//import org.openmole.tool.logger.Logger
//
//import scala.util.{ Failure, Success, Try }
//
//object SSHBatchJob extends Logger
//
//import org.openmole.plugin.environment.ssh.SSHBatchJob.Log._
//
//trait SSHBatchJob extends BatchJob {
//
//  val jobService: SSHJobService
//  def jobDescription: SSHJobDescription
//  var id: Option[Try[jobService.J]] = None
//
//  def submit = synchronized {
//    if (state != KILLED) {
//      id = Some(Try[jobService.J](jobService.submit(jobDescription)))
//      logger.fine(s"Submited job $id")
//    }
//  }
//
//  def updateState(implicit token: AccessToken) = synchronized {
//    id match {
//      case Some(Success(id)) ⇒
//        super.updateState(id)
//      case Some(Failure(e)) ⇒
//        state = FAILED
//        throw e
//      case None ⇒ SUBMITTED
//    }
//  }
//
//  def kill(implicit token: AccessToken) = synchronized {
//    id match {
//      case Some(Success(id)) ⇒ super.kill(id)
//      case _                 ⇒ state = KILLED
//    }
//  }
//
//  override def toString = id.toString
//
//}
