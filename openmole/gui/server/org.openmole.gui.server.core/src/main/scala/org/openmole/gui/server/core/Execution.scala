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

import java.io.OutputStream

import org.openmole.tool.collection._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.gui.ext.data._

class Execution {

  private lazy val moles = DataHandler[ExecutionId, Either[(StaticExecutionInfo, MoleExecution, StringBuilder), Failed]]()

  def add(key: ExecutionId, staticInfo: StaticExecutionInfo, moleExecution: MoleExecution, stringBuilder: StringBuilder) = moles.add(key, Left((staticInfo, moleExecution, stringBuilder)))

  def add(key: ExecutionId, error: Failed) = moles.add(key, Right(error))

  def cancel(key: ExecutionId) = get(key) match {
    case Some(Left((_, mE: MoleExecution, _))) ⇒ mE.cancel
    case _                                     ⇒
  }

  def remove(key: ExecutionId) = {
    cancel(key)
    moles.remove(key)
  }

  def staticInfo(key: ExecutionId): StaticExecutionInfo = get(key) match {
    case Some(Left((staticInfo, _, _))) ⇒ staticInfo
    case _                              ⇒ StaticExecutionInfo()
  }

  def executionInfo(key: ExecutionId): ExecutionInfo = get(key) match {
    case Some(Left((_, moleExecution: MoleExecution, stringBuilder: StringBuilder))) ⇒
      val d = moleExecution.duration.getOrElse(0L)
      moleExecution.exception match {
        case Some(t: Throwable) ⇒ Failed(ErrorBuilder(t))
        case _ ⇒
          if (moleExecution.canceled) Canceled()
          else if (moleExecution.finished) Finished()
          else if (moleExecution.started) {
            val out = stringBuilder.toString
            stringBuilder.clear
            Running(
              ready = moleExecution.ready,
              running = moleExecution.running,
              duration = d,
              completed = moleExecution.completed,
              environmentStates = moleExecution.environments.map {
                case (c, e) ⇒
                  EnvironmentState(c.task.name, e.running, e.done, e.submitted, e.failed)
              }.toSeq,
              lastOutputs = out
            )
          }
          else Unknown()
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

  private def get(key: ExecutionId): Option[Either[(StaticExecutionInfo, MoleExecution, StringBuilder), Failed]] = moles.get(key)
}