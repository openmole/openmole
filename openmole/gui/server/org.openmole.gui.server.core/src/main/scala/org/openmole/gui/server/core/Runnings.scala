package org.openmole.gui.server.core

import org.openmole.core.batch.environment.BatchEnvironment.BeginDownload
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.gui.ext.data._
import org.openmole.gui.server.core.Runnings.RunningEnvironment
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm._

/*
 * Copyright (C) 29/05/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Runnings {

  case class RunningEnvironment(environment: Environment, environmentError: List[EnvironmentError], networkActivity: NetworkActivity)
  def emptyRunningEnvironment(environment: Environment) = RunningEnvironment(environment, List(), NetworkActivity())

  lazy private val instance = new Runnings

  def append(envId: EnvironmentId,
             environment: Environment,
             todo: (RunningEnvironment) ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    val re = instance.runningEnvironments.getOrElse(envId, emptyRunningEnvironment(environment))
    instance.runningEnvironments(envId) = todo(re)
  }

  def addExecutionId(id: ExecutionId) = atomic { implicit ctx ⇒
    instance.ids(id) = Seq()
  }

  def add(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)], printStream: StringPrintStream) = atomic { implicit ctx ⇒
    addExecutionId(id)
    instance.outputs(id) = printStream
    envIds.foreach {
      case (envId, env) ⇒
        instance.ids(id) = instance.ids(id) :+ envId
        instance.runningEnvironments(envId) = RunningEnvironment(env, List(), NetworkActivity())
    }
  }

  def runningEnvironments(id: ExecutionId): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    runningEnvironments(ids.getOrElse(id, Seq()))
  }

  def runningEnvironments(envIds: Seq[EnvironmentId]): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    envIds.flatMap {
      id ⇒ instance.runningEnvironments.get(id).map(id -> _)
    }
  }

  def outputsDatas(id: ExecutionId, lines: Int) = atomic { implicit ctx ⇒
    RunningOutputData(id, instance.outputs(id).toString.lines.toSeq.takeRight(lines).mkString("\n"))
  }

  def ids = atomic { implicit ctx ⇒
    instance.ids
  }

  def remove(id: ExecutionId) = atomic { implicit ctx ⇒
    ids.remove(id).foreach {
      _.foreach {
        instance.runningEnvironments.remove
      }
    }
    instance.outputs.remove(id)
  }

}

class Runnings {

  val ids = TMap[ExecutionId, Seq[EnvironmentId]]()
  val outputs = TMap[ExecutionId, StringPrintStream]()
  val runningEnvironments = TMap[EnvironmentId, RunningEnvironment]()
}