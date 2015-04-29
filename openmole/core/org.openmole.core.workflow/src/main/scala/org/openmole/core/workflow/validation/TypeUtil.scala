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

package org.openmole.core.workflow.validation

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.obj.ClassUtils
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.tools.obj._

import scala.annotation.tailrec
import scala.collection.mutable.{ HashMap, HashSet, ListBuffer }

object TypeUtil {

  def unArrayify(t: PrototypeType[_]): (PrototypeType[_], Int) = {
    @tailrec def rec(c: PrototypeType[_], level: Int = 0): (PrototypeType[_], Int) =
      if (!c.isArray) (c, level)
      else rec(c.asArray.fromArray, level + 1)
    rec(t)
  }

  def receivedTypes(mole: Mole, sources: Sources, hooks: Hooks)(slot: Slot): Iterable[Prototype[_]] =
    validTypes(mole, sources, hooks)(slot).map { _.toPrototype }

  sealed trait ComputedType
  case class InvalidType(name: String, direct: Seq[PrototypeType[_]], toArray: Seq[PrototypeType[_]], fromArray: Seq[PrototypeType[_]]) extends ComputedType
  case class ValidType(name: String, `type`: PrototypeType[_], toArray: Boolean) extends ComputedType {
    def toPrototype =
      if (toArray) Prototype(name)(`type`.toArray)
      else Prototype(name)(`type`)
  }

  def validTypes(mole: Mole, sources: Sources, hooks: Hooks)(slot: Slot): Iterable[ValidType] = computeTypes(mole, sources, hooks)(slot).collect { case x: ValidType ⇒ x }

  def computeTypes(mole: Mole, sources: Sources, hooks: Hooks)(slot: Slot): Iterable[ComputedType] = {
    import ClassUtils._

    val (varNames, direct, toArray, fromArray) =
      computeTransmissions(mole, sources, hooks)(
        mole.inputTransitions(slot),
        mole.inputDataChannels(slot))

    varNames.toSeq.map {
      import scala.collection.mutable.ListBuffer.empty

      name ⇒
        (direct.getOrElse(name, empty), toArray.getOrElse(name, empty), fromArray.getOrElse(name, empty)) match {
          case (ListBuffer(d), ListBuffer(), ListBuffer()) ⇒ ValidType(name, d, false)
          case (ListBuffer(), ListBuffer(t), ListBuffer()) ⇒ ValidType(name, t.toArray, false)
          case (d, t, ListBuffer()) ⇒
            val allTypes = d.toList ++ t.map(_.toArray)
            val types = allTypes.distinct
            if (types.size == 1) ValidType(name, types.head, true)
            else InvalidType(name, d, t, Seq.empty)
          case (ListBuffer(), ListBuffer(), ListBuffer(f)) ⇒ ValidType(name, f.asArray.fromArray, false)
          case (d, t, f) ⇒ InvalidType(name, d, t, f)
        }
    }
  }

  private def computeTransmissions(mole: Mole, sources: Sources, hooks: Hooks)(transitions: Iterable[ITransition], dataChannels: Iterable[DataChannel]) = {
    val direct = new HashMap[String, ListBuffer[PrototypeType[_]]] // Direct transmission through transition or data channel
    val toArray = new HashMap[String, ListBuffer[PrototypeType[_]]] // Transmission through exploration transition
    val fromArray = new HashMap[String, ListBuffer[PrototypeType[_]]] // Transmission through aggregation transition
    val varNames = new HashSet[String]

    for (t ← transitions; d ← t.data(mole, sources, hooks)) {
      def explored = Explore.explored(t.start)
      def setFromArray =
        if (explored(d)) fromArray.getOrElseUpdate(d.name, new ListBuffer) += d.`type`
        else direct.getOrElseUpdate(d.name, new ListBuffer) += d.`type`

      varNames += d.name

      t match {
        case _: IAggregationTransition ⇒
          toArray.getOrElseUpdate(d.name, new ListBuffer) += d.`type`
        case _: IExplorationTransition ⇒ setFromArray
        case _: ISlaveTransition       ⇒ setFromArray
        case _                         ⇒ direct.getOrElseUpdate(d.name, new ListBuffer) += d.`type`
      }
    }

    for (dc ← dataChannels; d ← dc.data(mole, sources, hooks)) {
      varNames += d.name
      if (DataChannel.levelDelta(mole)(dc) >= 0) direct.getOrElseUpdate(d.name, new ListBuffer) += d.`type`
      else toArray.getOrElseUpdate(d.name, new ListBuffer) += d.`type`
    }
    (varNames.toSet, direct.toMap, toArray.toMap, fromArray.toMap)
  }
}
