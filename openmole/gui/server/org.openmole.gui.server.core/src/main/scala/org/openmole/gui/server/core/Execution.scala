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

  private lazy val moles = DataHandler[ExecutionId, MoleExecution]()

  val emptyState = States(Running(0, 0, 0), Finished())

  def add(key: ExecutionId, data: MoleExecution) = moles.add(key, data)

  def states(key: ExecutionId): States = {
    println("moles " + moles.getKeys)
    println("mole Key " + key)
    val moleExecution = get(key)
    moleExecution.map { me ⇒
      println("found id " + me.finished)
      States(Running(me.ready, me.running, me.completed), Finished())
    }.getOrElse({ println("Emppty"); emptyState })
  }

  def allStates: Seq[(ExecutionId, States)] = moles.getKeys.toSeq.map { id ⇒ id -> states(id) }

  def get(key: ExecutionId): Option[MoleExecution] = moles.get(key)
}