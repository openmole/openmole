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
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import scala.collection.immutable.{TreeSet, TreeMap}
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer


object TypeUtil {
 
  def receivedTypes(slot : ISlot): IDataSet = {
    val arrayManifest = spanArrayManifests(slot)
    new DataSet(
      arrayManifest._1.map{case(name, manifest) => toArray(new Prototype(name, manifest))}.map{new Data(_)} ++ 
      arrayManifest._2.map{case(name, manifest) => new Prototype(name, manifest)}.map{new Data(_)}
    )
  }
  
  
  def spanArrayManifests(slot : ISlot): (Map[String, Manifest[_]], Map[String, Manifest[_]]) = {
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]]
    
    val forceArray = new TreeSet[String] ++
      (for(t <- slot.transitions ; output <- t.unFiltred) yield {
        toArray.getOrElseUpdate(output.prototype.name, new ListBuffer[Manifest[_]]) += output.prototype.`type`
        if(classOf[IAggregationTransition].isAssignableFrom(t.getClass)) List(output.prototype.name)
        else List.empty
      }).flatten
    
    for(d <- slot.capsule.inputDataChannels.flatMap(_.data)) {
      toArray.getOrElseUpdate(d.prototype.name, new ListBuffer[Manifest[_]]) += d.prototype.`type`
    }
    
    val arrayAndOthers = toArray.span(elt => elt._2.size > 1 || forceArray.contains(elt._1))
    
    val toArrayManifest = TreeMap.empty[String, Manifest[_]] ++ 
      arrayAndOthers._1.map{case(name, manifests) => name -> intersectionArray(manifests map (_.erasure))} 
    val otherManifest = TreeMap.empty[String, Manifest[_]] ++ arrayAndOthers._2.map{case(name, manifests) => name -> manifests.head}
    (toArrayManifest, otherManifest)
  }
}
