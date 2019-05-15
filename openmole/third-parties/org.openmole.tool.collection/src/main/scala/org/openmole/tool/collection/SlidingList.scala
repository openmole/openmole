/**
 * Created by Romain Reuillon on 10/06/16.
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
 *
 */
package org.openmole.tool.collection

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._

class SlidingList[T] {

  private val queue = new ConcurrentLinkedQueue[T]()

  def put(t: T, capacity: Int) = queue.synchronized {
    while (queue.size() >= capacity) queue.poll()
    queue.add(t)
  }

  def elements: Vector[T] = queue.synchronized(queue.asScala.toVector)

  def clear() = synchronized {
    val v = elements
    queue.clear()
    v
  }

}
