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

import org.openmole.core.context.{ Val, ValType }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

import scala.collection.mutable.{ HashMap, HashSet, ListBuffer }

object TypeUtil {

  def receivedTypes(mole: Mole, sources: Sources, hooks: Hooks)(slot: TransitionSlot): Iterable[Val[_]] =
    validTypes(mole, sources, hooks)(slot).map { _.toPrototype }

  sealed trait ComputedType
  case class InvalidType(name: String, direct: Seq[ValType[_]], toArray: Seq[ValType[_]], fromArray: Seq[ValType[_]]) extends ComputedType
  case class ValidType(name: String, `type`: ValType[_], toArray: Boolean) extends ComputedType {
    def toPrototype =
      if (toArray) Val(name)(`type`.toArray)
      else Val(name)(`type`)
  }

  def validTypes(mole: Mole, sources: Sources, hooks: Hooks)(
    slot:        TransitionSlot,
    transition:  ITransition ⇒ Boolean = _ ⇒ true,
    dataChannel: DataChannel ⇒ Boolean = _ ⇒ true
  ): Iterable[ValidType] =
    computeTypes(mole, sources, hooks)(slot, transition).collect { case x: ValidType ⇒ x }

  def computeTypes(mole: Mole, sources: Sources, hooks: Hooks)(
    slot:        TransitionSlot,
    transition:  ITransition ⇒ Boolean = _ ⇒ true,
    dataChannel: DataChannel ⇒ Boolean = _ ⇒ true
  ): Iterable[ComputedType] = {
    val (varNames, direct, toArray, fromArray) =
      computeTransmissions(mole, sources, hooks)(
        mole.inputTransitions(slot).filter(transition),
        mole.inputDataChannels(slot).filter(dataChannel)
      )

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
          case (d, t, f)                                   ⇒ InvalidType(name, d, t, f)
        }
    }
  }

  private def computeTransmissions(mole: Mole, sources: Sources, hooks: Hooks)(transitions: Iterable[ITransition], dataChannels: Iterable[DataChannel]) = {
    val direct = new HashMap[String, ListBuffer[ValType[_]]] // Direct transmission through transition or data channel
    val toArray = new HashMap[String, ListBuffer[ValType[_]]] // Transmission through exploration transition
    val fromArray = new HashMap[String, ListBuffer[ValType[_]]] // Transmission through aggregation transition
    val varNames = new HashSet[String]

    for (t ← transitions; d ← t.data(mole, sources, hooks)) {
      def explored = ExplorationTask.explored(t.start)
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
