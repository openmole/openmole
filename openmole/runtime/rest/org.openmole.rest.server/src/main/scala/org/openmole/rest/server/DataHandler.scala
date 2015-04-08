/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.rest.server

import scala.concurrent.stm._

object DataHandler {
  def apply[K, V]() = new DataHandler[K, V] {}
}

trait DataHandler[K, V] {
  val map = TMap[K, V]()

  def add(key: K, data: V) = map.single put (key, data)

  def remove(key: K) = map.single remove key

  def get(key: K) = map.single get key

  def getKeys = map.single.keys

  def getOrElseUpdate(k: K, v: ⇒ V) = map.single.getOrElseUpdate(k, v)
}
