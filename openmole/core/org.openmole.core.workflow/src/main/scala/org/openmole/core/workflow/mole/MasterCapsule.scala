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

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.workflow.task._

object MasterCapsule {
  def apply(task: Task, persist: Seq[Val[_]], strain: Boolean) = new MasterCapsule(task, persist.map(_.name), strain)
  def apply(t: Task, persist: Val[_]*): MasterCapsule = apply(t, persist, false)
}

class MasterCapsule(task: Task, val persist: Seq[String] = Seq.empty, strainer: Boolean) extends Capsule(task, strainer) {
  def toPersist(context: Context): Context =
    persist.map { n ⇒ context(n) }

}