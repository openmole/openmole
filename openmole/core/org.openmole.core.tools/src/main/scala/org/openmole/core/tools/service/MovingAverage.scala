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

package org.openmole.core.tools.service

import scala.collection.mutable.Queue

class MovingAverage(period: Int, queue: Queue[Double]) {

  def this(period: Int, values: Double*) = this(period, Queue(values.slice(values.size - period, values.size): _*))

  def apply(n: Double) = synchronized {
    queue.enqueue(n)
    if (queue.size > period) queue.dequeue
  }

  def get = synchronized {
    queue.sum / queue.size
  }

  def size = queue.size

  override def toString = get.toString

  def reset(values: Double*) = synchronized {
    queue.clear
    queue ++= values.slice(values.size - period, values.size)
  }
}
