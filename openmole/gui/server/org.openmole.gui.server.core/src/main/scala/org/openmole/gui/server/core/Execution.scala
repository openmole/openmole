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
import org.openmole.tool.collection._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.gui.ext.data._
import org.openmole.tool.stream.StringPrintStream

case class DynamicExecutionInfo(moleExecution: MoleExecution,
                                outputStream: StringPrintStream)

class Execution {

  private lazy val moles = DataHandler[ExecutionId, Either[(StaticExecutionInfo, DynamicExecutionInfo), Failed]]()

  def add(key: ExecutionId, staticInfo: StaticExecutionInfo, dynamicExecutionInfo: DynamicExecutionInfo) =
    moles.add(key, Left((staticInfo, dynamicExecutionInfo)))

  def add(key: ExecutionId, error: Failed) = moles.add(key, Right(error))

  def cancel(key: ExecutionId) = {
    get(key) match {
      case Some(Left((_, dynamic: DynamicExecutionInfo))) ⇒
        println("cancel mE " + key)
        dynamic.moleExecution.cancel
      case x: Any ⇒ println("other ....")
    }
  }

  def remove(key: ExecutionId) = {
    cancel(key)
    moles.remove(key)
  }

  def staticInfo(key: ExecutionId): StaticExecutionInfo = get(key) match {
    case Some(Left((staticInfo, _))) ⇒ staticInfo
    case _                           ⇒ StaticExecutionInfo()
  }

  def environmentState(id: ExecutionId): Seq[EnvironmentState] = Runnings.runningEnvironments(id).map {
    case (id, e: Environment) ⇒
      EnvironmentState(
        id,
        e.toString,
        e.running,
        e.done,
        e.submitted,
        e.failed
      )
  }

  def executionInfo(key: ExecutionId): ExecutionInfo = get(key) match {
    case Some(Left((_, dynamic))) ⇒
      val moleExecution = dynamic.moleExecution
      val output = dynamic.outputStream

      val d = moleExecution.duration.getOrElse(0L)
      moleExecution.exception match {
        case Some(t: Throwable) ⇒ Failed(ErrorBuilder(t), duration = moleExecution.duration.getOrElse(0))
        case _ ⇒
          println("CAneceled" + key + " " + moleExecution.canceled)
          if (moleExecution.canceled) {
            println("canceled !!!")
            Canceled(duration = moleExecution.duration.get)
          }
          else if (moleExecution.finished)
            Finished(duration = moleExecution.duration.get,
              environmentStates = environmentState(key)
            )
          else if (moleExecution.started) {
            Running(
              ready = moleExecution.ready,
              running = moleExecution.running,
              duration = d,
              completed = moleExecution.completed,
              environmentStates = environmentState(key)
            )
          }
          else Ready()
      }
    case Some(Right(f: Failed)) ⇒ f
    case _                      ⇒ Failed(Error("Not found execution " + key))
  }

  def allStates: Seq[(ExecutionId, ExecutionInfo)] = moles.getKeys.toSeq.map { key ⇒
    key -> executionInfo(key)
  }

  def allStaticInfos: Seq[(ExecutionId, StaticExecutionInfo)] = moles.getKeys.toSeq.map { key ⇒
    key -> staticInfo(key)
  }

  private def get(key: ExecutionId) = moles.get(key)
}