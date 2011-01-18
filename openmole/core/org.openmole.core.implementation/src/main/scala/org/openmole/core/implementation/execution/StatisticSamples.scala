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

import scala.collection.mutable.HashMap
import org.openmole.core.model.execution.IStatisticSamples
import org.openmole.core.model.execution.SampleType
import scala.collection.mutable.ListBuffer

object StatisticSamples {
  val empty = new StatisticSamples(0)
}

class StatisticSamples(historySize: Int) extends IStatisticSamples {
 
  val averages = new HashMap[SampleType.Value, ListBuffer[Long]]

  override def apply(sample: SampleType.Value): Iterable[Long] = {
    averages.get(sample) match {
      case Some(av) => av
      case None => List.empty
    }
  }

  override def += (sample: SampleType.Value, length: Long) = synchronized {
      averages.get(sample) match {
        case Some(histo) => 
          if(histo.size < historySize) histo += length 
          else histo.tail += length
        case None => averages(sample) = ListBuffer(length)
      }
  }
}
