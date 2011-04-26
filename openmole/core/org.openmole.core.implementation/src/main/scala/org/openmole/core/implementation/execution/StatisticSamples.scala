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

package org.openmole.core.implementation.execution

import java.util.Collections
import java.util.LinkedList
import org.openmole.core.model.execution.IStatisticSample
import org.openmole.core.model.execution.IStatisticSamples
import scala.collection.JavaConversions._

object StatisticSamples {
  val empty = new StatisticSamples(0)
}

class StatisticSamples(historySize: Int) extends IStatisticSamples {
 
  val averages = Collections.synchronizedList(new LinkedList[IStatisticSample])

  override def iterator: Iterator[IStatisticSample] = averages.iterator

  override def += (sample: IStatisticSample) = synchronized {
    if(averages.size >= historySize) averages.remove(0)
    
    val it = averages.listIterator(averages.size)
    var inserted = false
    
    while(it.hasPrevious && !inserted) {
      val elt = it.previous
      if(elt.done <= sample.done) {
        it.add(sample)
        inserted = true
      }
    }
    
    if(!inserted) it.add(sample)
  }
}
