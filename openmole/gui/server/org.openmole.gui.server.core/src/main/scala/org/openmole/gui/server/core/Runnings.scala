package org.openmole.gui.server.core

import org.openmole.core.workflow.execution.Environment
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

  object RunningEnvironment {
    def empty(environment: Environment) = RunningEnvironment(environment, NetworkActivity())

  }

  case class RunningEnvironment(environment: Environment, networkActivity: NetworkActivity) {
    def environmentErrors(id: EnvironmentId) = environment.errors.map {
      ex ⇒
        EnvironmentError(id, ex.exception.getMessage, ErrorBuilder(ex.exception), ex.creationTime, Utils.javaLevelToErrorLevel(ex.level))
    }
  }

  lazy private val instance = new Runnings

  def append(envId: EnvironmentId,
             environment: Environment)(todo: (RunningEnvironment) ⇒ RunningEnvironment) = atomic { implicit ctx ⇒
    val re = instance.runningEnvironments.getOrElse(envId, RunningEnvironment.empty(environment))
    instance.runningEnvironments(envId) = todo(re)
  }

  def addExecutionId(id: ExecutionId) = atomic { implicit ctx ⇒
    instance.environmentIds(id) = Seq()
  }

  def add(id: ExecutionId, envIds: Seq[(EnvironmentId, Environment)], printStream: StringPrintStream) = atomic { implicit ctx ⇒
    addExecutionId(id)
    instance.outputs(id) = printStream
    envIds.foreach {
      case (envId, env) ⇒
        instance.environmentIds(id) = instance.environmentIds(id) :+ envId
        instance.runningEnvironments(envId) = RunningEnvironment.empty(env)
    }
  }

  def runningEnvironments(id: ExecutionId): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    runningEnvironments(environmentIds.getOrElse(id, Seq.empty))
  }

  def runningEnvironments(envIds: Seq[EnvironmentId]): Seq[(EnvironmentId, RunningEnvironment)] = atomic { implicit ctx ⇒
    envIds.flatMap {
      id ⇒ instance.runningEnvironments.get(id).map(id -> _)
    }
  }

  def outputsDatas(id: ExecutionId, lines: Int) = atomic { implicit ctx ⇒
    RunningOutputData(id, instance.outputs(id).toString.lines.toSeq.takeRight(lines).mkString("\n"))
  }

  def environmentIds = atomic { implicit ctx ⇒
    instance.environmentIds
  }

  def remove(id: ExecutionId) = atomic { implicit ctx ⇒
    environmentIds.remove(id).foreach {
      _.foreach {
        instance.runningEnvironments.remove
      }
    }
    instance.outputs.remove(id)
  }

}

class Runnings {
  val environmentIds = TMap[ExecutionId, Seq[EnvironmentId]]()
  val outputs = TMap[ExecutionId, StringPrintStream]()
  val runningEnvironments = TMap[EnvironmentId, RunningEnvironment]()
}