package org.openmole.gui.server.core

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

  case class RunningEnvironment(environment: Environment, environmentError: List[EnvironmentError])

  lazy private val instance = new Runnings

  def append(id: ExecutionId, envId: EnvironmentId, environment: Environment, exception: ExceptionRaised) = atomic { implicit ctx ⇒
    val envIds = add(id)
    instance.ids(id) = envIds :+ envId
    val re = instance.runningEnvironments.getOrElseUpdate(envId, RunningEnvironment(environment, List()))
    instance.runningEnvironments(envId) = re.copy(environmentError = EnvironmentError(envId, exception.exception.getMessage, ErrorBuilder(exception.exception)) :: re.environmentError)
  }

  def add(id: ExecutionId) = atomic { implicit ctx ⇒
    instance.ids.getOrElseUpdate(id, Seq())
  }

  def add(id: ExecutionId, printStream: StringPrintStream) = atomic { implicit ctx ⇒
    instance.outputs.getOrElseUpdate(id, printStream)
  }

  def runningEnvironments(id: ExecutionId): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    add(id)
    runningEnvironments(ids(id))
  }

  def runningEnvironments(envIds: Seq[EnvironmentId]): Seq[(EnvironmentId, RunningEnvironment)] =
    atomic { implicit ctx ⇒
      envIds.map { id ⇒
        instance.runningEnvironments.getOrElse(id, Seq())
        id -> instance.runningEnvironments(id)
      }
    }

  def outputsDatas(id: ExecutionId) = atomic { implicit ctx ⇒
    RunningOutputData(id, instance.outputs(id).toString)
  }

  def ids = instance.ids

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