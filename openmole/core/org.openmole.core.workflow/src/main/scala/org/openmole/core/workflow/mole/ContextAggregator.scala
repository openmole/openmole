/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.mole

import org.openmole.core.context.*
import org.openmole.core.exception.*

object ContextAggregator {

  def aggregateSimilar(contexts: Seq[Context]) =
    contexts.headOption match {
      case None    => Context.empty
      case Some(c) => aggregate(c.prototypes, c.prototypes.toSeq, contexts.flatMap(_.values))
    }

  def aggregate(prototypes: PrototypeSet, toArray: Seq[Val[?]], toAggregateList: Seq[Variable[?]]): Context = {
    val toAggregate = toAggregateList.groupBy { _.prototype.name }
    val toArrayMap = toArray.map(v => v.name -> v.`type`).toMap

    prototypes.foldLeft(List.empty[Variable[?]]) {
      case (acc, d) =>
        val merging = toAggregate.getOrElse(d.name, Iterable.empty)

        toArrayMap.get(d.name) match {
          case Some(arrayType) =>
            val array = arrayType.manifest.newArray(merging.size)
            merging.zipWithIndex.foreach {
              e =>
                try java.lang.reflect.Array.set(array, e._2, e._1.value)
                catch {
                  case t: Throwable =>
                    def valType = if (e._1.value != null) s" of type ${e._1.value.getClass}" else ""
                    throw new InternalProcessingError(s"Error setting value ${e._1.value}${valType} in an array ${array} of type ${array.getClass} at position ${e._2}", t)
                }
            }

            Variable(Val(d.name)(arrayType.toArray).asInstanceOf[Val[Any]], array) :: acc
          case None if !merging.isEmpty =>
            if (merging.size > 1) throw new InternalProcessingError("Variable " + d + " has been found multiple times, it doesn't match data flow specification, " + toAggregateList)
            Variable.unsecure(d, merging.head.value) :: acc
          case _ => acc
        }
    }
  }

}