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
  def apply(category: String, content: Long) = new Ticket(content, null)
  def apply(parent: Ticket, content: Long) = new Ticket(content, parent)

  implicit def ordering = Ordering.by[Ticket, Long](_.content)
}

class Ticket(val content: Long, _parent: Ticket) {
  def parent = Some(_parent)
  def parentOrException = parent.getOrElse(throw new InternalError("This is a root ticket, it has no parent."))

  def isRoot: Boolean = _parent == null

  override def equals(obj: Any): Boolean = content.equals(obj)
  override def hashCode = content.hashCode
}

