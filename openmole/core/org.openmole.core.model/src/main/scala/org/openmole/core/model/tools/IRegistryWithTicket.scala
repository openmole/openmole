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

package org.openmole.core.model.tools

import org.openmole.core.model.mole.ITicket
/**
 *
 * Registry to register value in fonction of a key and a ticket. The values are
 * stored in a WeakHashMap and are garbage collected after the ticket is gabage
 * collected.
 *
 * @author Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * @param <K> the type of the keys
 * @param <V> the type of the values
 */
trait IRegistryWithTicket[K, V] {

  /**
   *
   * Consult a value for a given key and ticket.
   *
   * @param key the index key
   * @param ticket the index ticket
   * @return the value or null if not found
   */
  def consult(key: K, ticket: ITicket): Option[V]

  /**
   *
   * Look if a value is registred for a given key and ticket.
   *
   * @param key the index key
   * @param ticket the index ticket
   * @return true if the value is present
   */
  def isRegistred(key: K, ticket: ITicket): Boolean

  /**
   *
   * Register a value for given key and ticket.
   *
   * @param key the index key
   * @param ticket the index ticket
   * @param val the value to register
   */
  def register(key: K, ticket: ITicket, value: V)

  /**
   *
   * Remove a value from the registry.
   *
   * @param key the index key
   * @param ticket the index ticket
   */
  def remove(key: K, ticket: ITicket): Option[V]

  def getOrElseUpdate(key: K, ticket: ITicket, f: ⇒ V): V
}
