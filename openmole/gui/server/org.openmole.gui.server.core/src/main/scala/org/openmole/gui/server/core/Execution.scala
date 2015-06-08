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

import org.openmole.tool.data._
import org.openmole.core.workflow.mole.MoleExecution
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.gui.ext.data._

object Execution {

  //implicit def execIdToMoleExeution(id: ExecutionId): Option[MoleExecution] = moles.get(id)

  private lazy val moles = DataHandler[ExecutionId, Either[MoleExecution, Failed]]()

  def add(key: ExecutionId, data: MoleExecution) = moles.add(key, Left(data))

  def add(key: ExecutionId, error: Failed) = moles.add(key, Right(error))

  def cancel(key: ExecutionId) = get(key) match {
    case Some(Left(mE: MoleExecution)) ⇒ mE.cancel
    case _                             ⇒
  }

  def remove(key: ExecutionId) = {
    cancel(key)
    moles.remove(key)
  }

  def executionInfo(key: ExecutionId): ExecutionInfo = get(key) match {
    case Some(Left(moleExecution: MoleExecution)) ⇒
      val d = moleExecution.duration.getOrElse(0L)
      if (moleExecution.canceled) Canceled()
      else if (moleExecution.finished) Finished()
      else if (moleExecution.started) Running(ready = moleExecution.ready, running = moleExecution.running, duration = d, completed = moleExecution.completed)
      else moleExecution.exception match {
        case Some(t: Throwable) ⇒ Failed(ErrorBuilder(t))
        case _                  ⇒ Unknown()
      }
    case Some(Right(f: Failed)) ⇒ f
    case _                      ⇒ Failed(Error("Not found execution " + key))
  }

  def allStates: Seq[(ExecutionId, ExecutionInfo)] = moles.getKeys.toSeq.map { key ⇒
    key -> executionInfo(key)
  }

  def get(key: ExecutionId): Option[Either[MoleExecution, Failed]] = moles.get(key)
}