/*
 * Copyright (C) 2011 Romain Reuillon
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

import org.openmole.core.model.mole._
import org.openmole.core.model.transition._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.DataChannel._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object TypeUtil {

  def receivedTypes(slot: ISlot): Iterable[IPrototype[_]] =
    computeManifests(slot).map {
      t ⇒
        if (t.toArray) new Prototype(t.name)(t.manifest.arrayManifest)
        else new Prototype(t.name)(t.manifest)
    }

  class ComputedType(val name: String, val manifest: Manifest[_], val toArray: Boolean)

  def computeManifests(slot: ISlot): Iterable[ComputedType] = {
    val (varNames, direct, toArray, fromArray) = computeManifests(slot.transitions, slot.inputDataChannels)

    varNames.map {
      import ListBuffer.empty

      name ⇒
        (direct.getOrElse(name, empty), toArray.getOrElse(name, empty), fromArray.getOrElse(name, empty)) match {
          case (ListBuffer(d), ListBuffer(), ListBuffer()) ⇒ new ComputedType(name, d, false)
          case (ListBuffer(), ListBuffer(t), ListBuffer()) ⇒ new ComputedType(name, t, true)
          case (d, t, ListBuffer()) ⇒ new ComputedType(name, s(d ++ t), true)
          case (ListBuffer(), ListBuffer(), ListBuffer(f)) ⇒
            if (f.isArray) new ComputedType(name, f.fromArray.toManifest, false)
            else new ComputedType(name, f, false)
          case (d, t, f) ⇒ throw new UserBadDataError("Type computation doesn't match specification, direct " + d + ", toArray " + t + ", fromArray " + f + " in " + slot)
        }
    }
  }

  private def s(m: Iterable[Manifest[_]]) = intersectionArray(m map (_.erasure))

  private def computeManifests(transitions: Iterable[ITransition], dataChannels: Iterable[IDataChannel]) = {
    val direct = new HashMap[String, ListBuffer[Manifest[_]]] // Direct transmission through transition or data channel
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]] // Transmission through exploration transition
    val fromArray = new HashMap[String, ListBuffer[Manifest[_]]] // Transmission through aggregation transition
    val varNames = new HashSet[String]

    for (t ← transitions; d ← t.data) {
      def setFromArray =
        if (d.mode is explore) fromArray.getOrElseUpdate(d.prototype.name, new ListBuffer[Manifest[_]]) += d.prototype.`type`
        else direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`

      varNames += d.prototype.name
      t match {
        case _: IAggregationTransition ⇒
          toArray.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
        case _: IExplorationTransition ⇒ setFromArray
        case _: ISlaveTransition ⇒ setFromArray
        case _ ⇒ direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      }
    }

    for (dc ← dataChannels; d ← dc.data) {
      varNames += d.prototype.name
      if (DataChannel.levelDelta(dc) >= 0) direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      else toArray.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
    }

    (varNames.toSet, direct.toMap, toArray.toMap, fromArray.toMap)
  }
}
