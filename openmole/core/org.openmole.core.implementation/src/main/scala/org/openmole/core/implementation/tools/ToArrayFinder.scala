/*
 * Copyright (C) 2011 reuillon
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

import org.openmole.core.model.transition.ISlot
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.IAggregationTransition
import scala.collection.immutable.TreeMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

object ToArrayFinder {
 
  def toArrayManifests(slot : ISlot) = {
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]]
    var forceArray = new TreeSet[String]
    
    for(t <- slot.transitions ; output <- t.unFiltred) {
      toArray.getOrElseUpdate(output.prototype.name, new ListBuffer[Manifest[_]]) += output.prototype.`type`
      if(classOf[IAggregationTransition].isAssignableFrom(t.getClass)) forceArray += output.prototype.name
    }
       
    for(d <- slot.capsule.inputDataChannels.flatMap(_.data)) {
      toArray.getOrElseUpdate(d.prototype.name, new ListBuffer[Manifest[_]]) += d.prototype.`type`
    }
    
    TreeMap.empty[String, Manifest[_]] ++ toArray.filter(elt => elt._2.size > 1 || forceArray.contains(elt._1)).map{elt => elt._1 -> intersection(elt._2: _*)}
  }
}
