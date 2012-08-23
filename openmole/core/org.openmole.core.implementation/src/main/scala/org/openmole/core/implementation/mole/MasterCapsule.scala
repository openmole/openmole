/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.mole

import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.task.ITask
import org.openmole.core.implementation.data.Context._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.mole.IMasterCapsule
import org.openmole.misc.exception.UserBadDataError

class MasterCapsule(_task: Option[ITask] = None, val persist: Set[String] = Set.empty) extends Capsule(_task) with IMasterCapsule {

  def this(t: ITask, persist: String*) = this(Some(t), persist.toSet)

  def this(t: ITask, head: IPrototype[_], persist: IPrototype[_]*) = this(Some(t), (head :: persist.toList).map { _.name }.toSet)

  def this(t: ITask, head: String, persist: Array[String]) = this(t, (head :: persist.toList): _*)

  def this(t: ITask, head: IPrototype[_], persist: Array[IPrototype[_]]) = this(t, head, persist: _*)

  override def toPersist(context: IContext) = persist.flatMap { n ⇒ context.variable(n).toList }.toContext

}