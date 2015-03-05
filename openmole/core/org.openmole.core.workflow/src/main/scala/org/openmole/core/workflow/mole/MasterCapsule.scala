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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object MasterCapsule {
  def apply(task: Task, persist: Seq[String] = Seq.empty, strainer: Boolean = false) = new MasterCapsule(task, persist, strainer)
  def apply(t: Task, persist: String*): MasterCapsule = apply(t, persist)
  def apply(t: Task, head: Prototype[_], persist: Prototype[_]*): MasterCapsule = apply(t, (head :: persist.toList).map { _.name })
}

class MasterCapsule(task: Task, val persist: Seq[String] = Seq.empty, strainer: Boolean) extends Capsule(task, strainer) {
  def toPersist(context: Context) = persist.flatMap { n ⇒ context.variable(n).toList }.toContext
}