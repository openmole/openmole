package org.openmole.gui.server.core

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

import org.openmole.core.workflow.execution.Environment
import org.openmole.gui.server.core.Runnings.RunningEnvironment
import org.openmole.tool.collection._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.gui.ext.data._
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm._

case class DynamicExecutionInfo(
  moleExecution: MoleExecution,
  outputStream:  StringPrintStream
)

class Execution {

  private lazy val staticExecutionInfo = TMap[ExecutionId, StaticExecutionInfo]()
  private lazy val dynamicExecutionInfo = TMap[ExecutionId, DynamicExecutionInfo]()
  private lazy val errors = TMap[ExecutionId, Failed]()

  def addStaticInfo(key: ExecutionId, staticInfo: StaticExecutionInfo) = atomic { implicit ctx ⇒
    staticExecutionInfo(key) = staticInfo
  }

  def addDynamicInfo(key: ExecutionId, info: DynamicExecutionInfo) = atomic { implicit ctx ⇒
    if (staticExecutionInfo.contains(key)) {
      dynamicExecutionInfo(key) = info
      true
    }
    else false
  }

  def addError(key: ExecutionId, error: Failed) = atomic { implicit ctx ⇒
    errors(key) = error
  }

  def cancel(key: ExecutionId) =
    dynamicExecutionInfo.single.get(key) match {
      case Some(dynamic) ⇒ dynamic.moleExecution.cancel
      case _             ⇒
    }

  def remove(key: ExecutionId) = {
    cancel(key)
    atomic { implicit ctx ⇒
      Runnings.remove(key)
      staticExecutionInfo.remove(key)
      dynamicExecutionInfo.remove(key)
      errors.remove(key)
    }
  }

  def environmentState(id: ExecutionId): Seq[EnvironmentState] =
    Runnings.runningEnvironments(id).map {
      case (envId, e) ⇒ {
        EnvironmentState(
          envId,
          e.environment.simpleName,
          e.environment.running,
          e.environment.done,
          e.environment.submitted,
          e.environment.failed,
          e.networkActivity,
          e.executionActivty
        )
      }
    }

  def executionInfo(key: ExecutionId): ExecutionInfo = atomic { implicit ctx ⇒
    (errors.get(key), dynamicExecutionInfo.get(key)) match {
      case (Some(error), _) ⇒ error
      case (_, Some(dynamic)) ⇒
        val moleExecution = dynamic.moleExecution
        val output = dynamic.outputStream

        val d = moleExecution.duration.getOrElse(0L)
        moleExecution.exception match {
          case Some(t) ⇒ Failed(ErrorBuilder(t.exception), environmentStates = environmentState(key), duration = moleExecution.duration.getOrElse(0))
          case _ ⇒
            if (moleExecution.canceled) Canceled(environmentStates = environmentState(key), duration = moleExecution.duration.get)
            else if (moleExecution.finished)
              Finished(
                duration = moleExecution.duration.get,
                completed = moleExecution.jobStatuses.completed,
                environmentStates = environmentState(key)
              )
            else if (moleExecution.started) {
              val statuses = moleExecution.jobStatuses
              Running(
                ready = statuses.ready,
                running = statuses.running,
                duration = d,
                completed = statuses.completed,
                environmentStates = environmentState(key)
              )
            }
            else Ready()
        }
      case _ ⇒ Ready()
    }
  }

  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)] = atomic { implicit ctx ⇒
    for {
      (k, s) ← staticExecutionInfo.toSeq
    } yield (k, s)
  }

  def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[RunningOutputData]) = atomic { implicit ctx ⇒

    val envIds = Runnings.environmentIds

    val outputs = envIds.keys.toSeq.map {
      Runnings.outputsDatas(_, lines)
    }

    val executions = for {
      (k, s) ← staticExecutionInfo.toSeq
    } yield (k, executionInfo(k))

    (executions, outputs)
  }

}