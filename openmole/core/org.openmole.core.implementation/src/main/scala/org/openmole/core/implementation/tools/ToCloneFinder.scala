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

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet


object ToCloneFinder {

  //TODO Improvement condition are evaluated several times
  def variablesToClone(caps: IGenericCapsule, global: IContext, context: IContext): Set[String] = {

    class DataInfo {
      val nbUsage = new AtomicInteger
      val nbMutable = new AtomicInteger
    }

    var counters = new TreeMap[String, DataInfo]

    for (transition <- caps.outputTransitions) {
      if (transition.isConditionTrue(global, context)) {
        transition.end.capsule.task match {
          case Some(t) =>
              
            for (data <- t.inputs) {
              val name = data.prototype.name

              val info = counters.get(name) match {
                case None => 
                  val i = new DataInfo
                  counters += ((name, i))
                  i
                case Some(i) => i
              }

              info.nbUsage.incrementAndGet
              if (!data.mode.isImmutable) {
                info.nbMutable.incrementAndGet
              }
            }
          case None =>
        }
      }
    }

    for (channel <- caps.outputDataChannels) {
      for (data <- channel.data) {
        val name = data.prototype.name

        val info = counters.get(name) match {
          case None =>
            val i = new DataInfo
            counters += ((name, i))
            i
          case Some(i) => i
        }

        info.nbUsage.incrementAndGet
        if (!data.mode.isImmutable)  info.nbMutable.incrementAndGet
                
      }
    }

    var toClone = new TreeSet[String]

    for(entry <- counters) {
      if(entry._2.nbUsage.get > 1 && entry._2.nbMutable.get > 0) {
        toClone += entry._1
      }
    }

    return toClone
  }
}
