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
import org.openmole.misc.tools.obj._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import collection.mutable

object TypeUtil {

  def intersect(d: Iterable[Iterable[Prototype[_]]]) = {

    def superType(d: Iterable[Prototype[_]]) =
      ClassUtils.intersectionArray(d.map { _.`type`.runtimeClass })

    val indexedD = d.flatten.toList.groupBy(_.name)

    val r: Iterable[Option[Prototype[_]]] = indexedD.map {
      case (name, prototypes) ⇒
        if (prototypes.size == d.size) Some(Prototype(name)(superType(prototypes))) else None
    }
    r.flatten
  }

  def receivedTypes(mole: IMole, sources: Sources, hooks: Hooks)(slot: Slot): Iterable[Prototype[_]] =
    computeManifests(mole, sources, hooks)(slot).map { _.toPrototype }

  class ComputedType(val name: String, val manifest: Manifest[_], val toArray: Boolean, val isOptional: Boolean) {
    def toPrototype =
      if (toArray) Prototype(name)(manifest.arrayManifest)
      else Prototype(name)(manifest)
  }

  def computeManifests(mole: IMole, sources: Sources, hooks: Hooks)(slot: Slot): Iterable[ComputedType] = {
    import ClassUtils._

    val (varNames, direct, toArray, fromArray, optional) =
      computeTransmissions(mole, sources, hooks)(
        mole.inputTransitions(slot),
        mole.inputDataChannels(slot))

    varNames.map {
      import ListBuffer.empty

      name ⇒
        (direct.getOrElse(name, empty), toArray.getOrElse(name, empty), fromArray.getOrElse(name, empty)) match {
          case (ListBuffer(d), ListBuffer(), ListBuffer()) ⇒ new ComputedType(name, d, false, optional(name))
          case (ListBuffer(), ListBuffer(t), ListBuffer()) ⇒ new ComputedType(name, t, true, optional(name))
          case (d, t, ListBuffer()) ⇒ new ComputedType(name, s(d ++ t), true, optional(name))
          case (ListBuffer(), ListBuffer(), ListBuffer(f)) ⇒
            if (f.isArray) new ComputedType(name, f.fromArray.toManifest, false, optional(name))
            else new ComputedType(name, f, false, optional(name))
          case (d, t, f) ⇒ throw new UserBadDataError("Type computation doesn't match specification, direct " + d + ", toArray " + t + ", fromArray " + f + " in " + slot)
        }
    }
  }

  private def s(m: Iterable[Manifest[_]]) = ClassUtils.intersectionArray(m map (_.runtimeClass))

  private def computeTransmissions(mole: IMole, sources: Sources, hooks: Hooks)(transitions: Iterable[ITransition], dataChannels: Iterable[IDataChannel]) = {
    val direct = new HashMap[String, ListBuffer[Manifest[_]]] // Direct transmission through transition or data channel
    val toArray = new HashMap[String, ListBuffer[Manifest[_]]] // Transmission through exploration transition
    val fromArray = new HashMap[String, ListBuffer[Manifest[_]]] // Transmission through aggregation transition
    val optional = new HashMap[String, ListBuffer[Boolean]]
    val varNames = new HashSet[String]

    for (t ← transitions; d ← t.data(mole, sources, hooks)) {
      def setFromArray =
        if (d.mode is Explore) fromArray.getOrElseUpdate(d.prototype.name, new ListBuffer[Manifest[_]]) += d.prototype.`type`
        else direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`

      varNames += d.prototype.name

      t match {
        case _: IAggregationTransition ⇒
          toArray.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
        case _: IExplorationTransition ⇒ setFromArray
        case _: ISlaveTransition ⇒ setFromArray
        case _ ⇒ direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      }

      optional.getOrElseUpdate(d.prototype.name, new ListBuffer) += (d.mode is Optional)
    }

    for (dc ← dataChannels; d ← dc.data(mole, sources, hooks)) {
      varNames += d.prototype.name
      if (DataChannel.levelDelta(mole)(dc) >= 0) direct.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      else toArray.getOrElseUpdate(d.prototype.name, new ListBuffer) += d.prototype.`type`
      optional.getOrElseUpdate(d.prototype.name, new ListBuffer) += (d.mode is Optional)
    }
    val optionalMap = optional.map { case (k, v) ⇒ k -> v.forall(_ == true) }.withDefault(s ⇒ false).toMap
    (varNames.toSet, direct.toMap, toArray.toMap, fromArray.toMap, optionalMap)
  }
}
