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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.tools

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.model.data.IData
import org.openmole.core.model.mole.IMoleExecution
import java.util.logging.Logger
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.task.ITask
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import MutableComputing.Mutable._


object ToCloneFinder {

  private class DataInfo {
    var nbUsage = 0
    var nbMutable = 0
    var forceCloning = false
  }

  //TODO Improvement condition are evaluated several times
  def variablesToClone(caps: ICapsule, context: IContext, moleExecution: IMoleExecution): Set[String] = {

    var counters = new TreeMap[String, DataInfo]
    val levelComputing = LevelComputing(moleExecution)
    for (transition <- caps.outputTransitions) {
      if (transition.isConditionTrue(context)) {
        transition.end.capsule.task match {
          case Some(t) =>
            for (data <- t.inputs) {
              val name = data.prototype.name

              val info = counters.get(name) match {
                case None => 
                  val info = new DataInfo
                  counters += ((name, info))
                  info
                case Some(info) => info
              }
              updateInfo(caps, info, data, MutableComputing(moleExecution), levelComputing)        
             
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

        updateInfo(caps, info, data, MutableComputing(moleExecution), levelComputing)        
      }
    }

    var toClone = new TreeSet[String]

    for(entry <- counters) {
      if(entry._2.forceCloning || (entry._2.nbUsage > 1 && entry._2.nbMutable > 0)) {
        toClone += entry._1
      }
    }

    return toClone
  }
  
  
  private def updateInfo(caps: ICapsule, info: DataInfo, data: IData[_], mutableComputing: MutableComputing, levelComputing: LevelComputing) = {
    info.nbUsage += 1
    mutableComputing.mutable(caps, data.prototype, levelComputing) match {
      case No =>
      case Yes => 
        info.nbMutable += 1
      case YesForHigherLevel =>
        info.nbMutable += 1
        info.forceCloning = true
    }
  }
}
