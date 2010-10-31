/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import org.openmole.core.model.execution.IStatistic
import scala.collection.mutable.HashMap
import org.openmole.core.model.execution.SampleType

object Statistic {
  val empty = new Statistic(0)
}

class Statistic(historySize: Int) extends IStatistic {
 
  val averages = new HashMap[SampleType, List[Long]]

  override def apply(sample: SampleType): Iterable[Long] = {
    averages.get(sample) match {
      case Some(av) => av
      case None => List.empty
    }
  }

  override def add (sample: SampleType, length: Long) = {
    synchronized {
      averages(sample) = averages.get(sample) match {
        case Some(histo) => 
          if(histo.size < historySize) histo :+ length 
          else histo.tail :+ length
        case None => List(length)
      }
    }
  }
}
