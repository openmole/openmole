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

package org.openmole.core.implementation.tools

import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.tools.IRegistryWithTicket
import scala.collection.mutable.{ WeakHashMap, HashMap, SynchronizedMap }

class RegistryWithTicket[K, V] extends IRegistryWithTicket[K, V] {

  class Registry extends HashMap[K, V] with SynchronizedMap[K, V]

  val registries = new WeakHashMap[ITicket, Registry]

  def registry(ticket: ITicket): Registry = synchronized {
    registries.getOrElseUpdate(ticket, new Registry)
  }

  override def consult(key: K, ticket: ITicket): Option[V] = synchronized {
    registry(ticket)(key)
  }

  override def isRegistred(key: K, ticket: ITicket): Boolean = synchronized {
    registry(ticket).contains(key)
  }

  override def register(key: K, ticket: ITicket, value: V) = synchronized {
    registry(ticket) += (key -> value)
  }

  override def remove(key: K, ticket: ITicket): Option[V] = synchronized {
    var ret = registry(ticket).remove(key)
    if (registries(ticket).isEmpty) registries -= ticket
    ret
  }

  override def getOrElseUpdate(key: K, ticket: ITicket, f: ⇒ V): V = synchronized {
    registries.getOrElseUpdate(ticket, new Registry).getOrElseUpdate(key, f)
  }

}
