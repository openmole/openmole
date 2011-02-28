/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.implementation.tools

import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.implementation.capsule.GenericCapsule._
import org.openmole.core.model.transition.IAggregationTransition
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object ToArrayFinder {
  
  def toArrayManifests(caps: IGenericCapsule) = {
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]]
    var forceArray = new TreeSet[String]
    
    for(t <- caps.intputSlots.flatMap(_.transitions) ; output <- t.start.decapsulate.userOutputs){
      if(!t.filtered.contains(output.prototype.name)) {
        toArray.getOrElseUpdate(output.prototype.name, new ListBuffer[Manifest[_]]) += output.prototype.`type`
        if(classOf[IAggregationTransition].isAssignableFrom(t.getClass)) forceArray += output.prototype.name
      }
    }
       
    for(d <- caps.inputDataChannels.flatMap(_.data)) {
      toArray.getOrElseUpdate(d.prototype.name, new ListBuffer[Manifest[_]]) += d.prototype.`type`
    }
    
    TreeMap.empty[String, Manifest[_]] ++ toArray.filter(elt => elt._2.size > 1 || forceArray.contains(elt._1)).map{elt => elt._1 -> intersection( elt._2: _*)}
  }
}
