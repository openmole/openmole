/*
 * Copyright (C) 28/11/12 Romain Reuillon
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

package org.openmole.core.workflow.transition

import org.openmole.core.context.Val

object BlockList {
  def empty = new BlockList {
    override def apply(t: Val[?]) = false
  }

  implicit def seqOfValToKeep(s: Seq[Val[?]]): Keep = Keep(s *)
}

trait BlockList extends (Val[?] => Boolean)

trait Block extends BlockList {
  def filtered: Set[String]
  override def apply(t: Val[?]) = filtered.contains(t.name)
}

trait Keep extends BlockList {
  def kept: Set[String]
  override def apply(t: Val[?]) = !kept.contains(t.name)
}

object Block {

  def apply(ts: Val[?]*): Block = new Block {
    val filtered: Set[String] = ts.map(_.name).toSet
  }

}

object Keep {

  def apply(ts: Val[?]*): Keep = new Keep {
    val kept = ts.map(_.name).toSet
  }
}