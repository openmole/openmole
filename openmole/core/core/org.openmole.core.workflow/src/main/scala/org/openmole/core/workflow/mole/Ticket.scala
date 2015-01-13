/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

object Ticket {
  def apply(category: String, content: Long) = {
    new Ticket(category, content, None)
  }

  def apply(parent: Ticket, content: Long) = {
    new Ticket(parent.category, content, Some(parent))
  }

  implicit def ordering = new Ordering[Ticket] {
    override def compare(left: Ticket, right: Ticket): Int = {
      val compare = left.content.compare(right.content)
      if (compare != 0) return compare
      left.category.compare(right.category)
    }
  }
}

class Ticket(val category: String, val content: Long, val parent: Option[Ticket]) {
  def parentOrException = parent.getOrElse(throw new InternalError("This is a root ticket, it has no parent."))

  def isRoot: Boolean = parent.equals(None)

  override def equals(obj: Any): Boolean = (content, category).equals(obj)
  override def hashCode = (content, category).hashCode
}

