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

package org.openmole.core.implementation.validation

import org.openmole.core.model.transition.IExplorationTransition
import org.openmole.core.model.transition.ISlot
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.DataChannel._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer


object TypeUtil {
 
  def receivedTypes(slot : ISlot): Iterable[IPrototype[_]] =
    computeManifests(slot).map {
      t =>
        if(t.toArray) new Prototype(t.name, t.manifest.arrayManifest)
        else new Prototype(t.name, t.manifest)
    }
  
  class ComputedType(val name: String, val manifest: Manifest[_], val toArray: Boolean)
  
  def computeManifests(slot : ISlot): Iterable[ComputedType] = {
    val direct = new HashMap[String, ListBuffer[Manifest[_]]]
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]]
    val fromArray = new HashMap[String, ListBuffer[Manifest[_]]]
    val varNames = new HashSet[String]
    
    for(t <- slot.transitions ; output <- t.unFiltred) {
      varNames += output.prototype.name
      t match {
        case _: IAggregationTransition => 
          toArray.getOrElseUpdate(output.prototype.name, new ListBuffer) += output.prototype.`type` 
        case _: IExplorationTransition => 
          if(output.mode is explore) fromArray.getOrElseUpdate(output.prototype.name, new ListBuffer[Manifest[_]]) += output.prototype.`type`
          else direct.getOrElseUpdate(output.prototype.name, new ListBuffer) += output.prototype.`type` 
        case _ => direct.getOrElseUpdate(output.prototype.name, new ListBuffer) += output.prototype.`type` 
      }
    }
    
    for(dc <- slot.capsule.inputDataChannels) {
      for(d <- dc.data) {
        varNames += d.prototype.name
        if(DataChannel.levelDelta(dc) >= 0) direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
        else toArray.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      }
    }
    
    def s(m: Iterable[Manifest[_]]) = intersectionArray(m map (_.erasure))
    
    varNames.map {
      import ListBuffer.empty

      name =>
        (direct.getOrElse(name, empty), toArray.getOrElse(name, empty), fromArray.getOrElse(name, empty)) match {
          case (ListBuffer(d), ListBuffer(), ListBuffer()) => new ComputedType(name, d, false)
          case (ListBuffer(), ListBuffer(t), ListBuffer()) => new ComputedType(name, t, true)
          case (d, t, ListBuffer()) => new ComputedType(name, s(d ++ t), true)
          case (ListBuffer(), ListBuffer(), ListBuffer(f)) =>
            if(f.isArray) new ComputedType(name, f.fromArray.toManifest, false)
            else new ComputedType(name, f, false)
          case (d, t, f) => throw new UserBadDataError("Type computation doesn't match specification, direct " + d + ", toArray " + t + ", fromArray " + f)
        }
    }
    
   /* val multiplesAndOthers = allPrototypes.span(elt => elt._2.size > 1) // || forceArray.contains(elt._1))

    val toArrayManifest = TreeMap.empty[String, Manifest[_]] ++ 
      arrayAndOthers._1.map{case(name, manifests) => name -> intersectionArray(manifests map (_.erasure))} 
    val otherManifest = TreeMap.empty[String, Manifest[_]] ++ arrayAndOthers._2.map{case(name, manifests) => name -> manifests.head}
    (toArrayManifest, otherManifest)*/
  }
}
