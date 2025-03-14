//package org.openmole.core.workflow.test
//
//import org.openmole.core.workflow.execution.{Environment, EnvironmentBuilder, ExceptionEvent, ExecutionJob, LocalEnvironment, SubmissionEnvironment}
//import org.openmole.core.workflow.job.JobGroup
//import org.openmole.core.workflow.task.Task
//
///*
// * Copyright (C) 2025 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//object TestEnvironment:
//  def apply() =
//    val env = new TestEnvironment()
//    EnvironmentBuilder: _ =>
//      env
//
//class TestEnvironment extends SubmissionEnvironment:
//  val env = new LocalEnvironment(1, false, None, false)
//
//  override def jobs: Iterable[ExecutionJob] = Seq()
//  override def runningJobs: Seq[ExecutionJob] = Seq()
//  override def ready: Long = 0
//  override def clean: Boolean = true
//  override def errors: Seq[ExceptionEvent] = Environment.errors(env)
//  override def clearErrors: Seq[ExceptionEvent] = Environment.clearErrors(env)
//  override def submitted: Long = env.submitted
//  override def running: Long = env.running
//  override def done: Long = env.done
//  override def failed: Long = env.failed
//  override def start(): Unit = env.start()
//  override def stop(): Unit = env.stop()
//
//  override def name: Option[String] = Some("test")
//
//
