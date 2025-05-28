package org.openmole.tool.collection

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

/*
 * Copyright (C) 2025 Romain Reuillon
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

class DisposableRepository[K, V](build: K => V):
  val objects = new mutable.HashMap[K, (V, AtomicInteger)]

  private def get(k: K) = objects.synchronized:
    val (lock, users) = objects.getOrElseUpdate(k, (build(k), new AtomicInteger(0)))
    users.incrementAndGet
    lock

  private def dispose(k: K) = objects.synchronized:
    objects.get(k) match
      case Some((v, users)) =>
        val value = users.decrementAndGet
        if value <= 0 then objects.remove(k)
        v
      case None => throw new IllegalArgumentException("Disposing an object that has not been borrowed.")
  
  def borrow[T](k: K)(f: V => T): T =
    val v = get(k)
    try f(v)
    finally dispose(k)
