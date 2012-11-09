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

import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.job._

object MasterCapsule {
  def apply(task: ITask, persist: Set[String] = Set.empty) = new MasterCapsule(task, persist)
  def apply(t: ITask, persist: String*): MasterCapsule = apply(t, persist.toSet)
  def apply(t: ITask, head: Prototype[_], persist: Prototype[_]*): MasterCapsule = apply(t, (head :: persist.toList).map { _.name }.toSet)
}

class MasterCapsule(task: ITask, val persist: Set[String] = Set.empty) extends Capsule(task) with IMasterCapsule {
  override def toPersist(context: Context) = persist.flatMap { n ⇒ context.variable(n).toList }.toContext
}