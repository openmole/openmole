/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.implementation.tools

import org.openmole.core.model.job.ITicket
import org.openmole.core.model.tools.IRegistryWithTicket
import scala.collection.mutable.WeakHashMap

class RegistryWithTicket[K, V] extends IRegistryWithTicket[K, V] {

  val registries = new WeakHashMap[ITicket, IRegistry[K, V]]

  def registry(ticket: ITicket): IRegistry[K, V] = synchronized {
    registries.getOrElse(ticket, {val ret = new Registry[K, V]
                                  registries(ticket) = ret
                                  ret}
    )        
  }

  override def consult(key: K, ticket: ITicket): Option[V] = {
    registry(ticket)(key)
  }

  override def isRegistred(key: K, ticket: ITicket): Boolean = {
    registry(ticket).isRegistred(key)
  }

  override def register(key: K, ticket: ITicket, value: V) = {
    registry(ticket) += (key, value)
  }

  override def remove(key: K, ticket: ITicket): Option[V] = {
    registry(ticket).remove(key)
  }
}
