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

import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.mole.*

import scala.collection.mutable

/**
 *
 * Registry to register value in function of a key and a ticket. The values are
 * stored in a WeakHashMap and are garbage collected after the ticket is gabage
 * collected.
 *
 * @author Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * @tparam K the type of the keys
 * @tparam V the type of the values
 */


object RegistryWithTicket:

  def apply[K, V](): RegistryWithTicket[K, V] = new mutable.WeakHashMap[Ticket, mutable.HashMap[K, V]]

  extension [K, V](registries: RegistryWithTicket[K, V])
    def registry(ticket: Ticket): mutable.HashMap[K, V] = registries.synchronized:
      registries.getOrElseUpdate(ticket, new mutable.HashMap[K, V]())

    /**
     *
     * Consult a value for a given key and ticket.
     *
     * @param key the index key
     * @param ticket the index ticket
     * @return the value or null if not found
     */
    def consult(key: K, ticket: Ticket): Option[V] = registries.synchronized:
      registry(ticket).get(key)

    /**
     *
     * Look if a value is registred for a given key and ticket.
     *
     * @param key the index key
     * @param ticket the index ticket
     * @return true if the value is present
     */
    def isRegistred(key: K, ticket: Ticket): Boolean = registries.synchronized:
      registry(ticket).contains(key)


    /**
    *
    * Register a value for given key and ticket.
    *
    * @param key the index key
    * @param ticket the index ticket
    * @param value the value to register
    */
    def register(key: K, ticket: Ticket, value: V) = registries.synchronized:
      registries.registry(ticket) += (key -> value)


    /**
     *
     * Remove a value from the registry.
     *
     * @param key the index key
     * @param ticket the index ticket
     */
    def remove(key: K, ticket: Ticket): Option[V] = registries.synchronized:
      val ret = registry(ticket).remove(key)
      if (registries(ticket).isEmpty) registries -= ticket
      ret


    def getOrElseUpdate(key: K, ticket: Ticket, f: => V): V = registries.synchronized:
      registries.getOrElseUpdate(ticket, new mutable.HashMap[K, V]()).getOrElseUpdate(key, f)


opaque type RegistryWithTicket[K, V] = mutable.WeakHashMap[Ticket, mutable.HashMap[K, V]]

