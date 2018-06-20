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

package org.openmole.core.workflow.tools

import org.openmole.core.context._
import org.openmole.core.exception._

object ContextAggregator {

  def aggregate(prototypes: PrototypeSet, toArray: PartialFunction[String, ValType[_]], toAggregateList: Iterable[(Long, Variable[_])]): Context = {
    val toAggregate = toAggregateList.groupBy { case (_, v) ⇒ v.prototype.name }

    prototypes.foldLeft(List.empty[Variable[_]]) {
      case (acc, d) ⇒
        val merging = if (toAggregate.isDefinedAt(d.name)) toAggregate(d.name).toSeq.sortBy { case (i, _) ⇒ i }.map(_._2) else Iterable.empty

        if (toArray.isDefinedAt(d.name)) {
          val `type` = toArray(d.name)
          val array = `type`.manifest.newArray(merging.size)
          merging.zipWithIndex.foreach {
            e ⇒
              try java.lang.reflect.Array.set(array, e._2, e._1.value)
              catch {
                case t: Throwable ⇒
                  def valType = if (e._1.value != null) " of type ${e._1.value.getClass}" else ""
                  throw new InternalProcessingError(s"Error setting value ${e._1.value}${valType} in an array ${array} of type ${array.getClass} at position ${e._2}", t)
              }
          }
          Variable(Val(d.name)(`type`.toArray).asInstanceOf[Val[Any]], array) :: acc
        }
        else if (!merging.isEmpty) {
          if (merging.size > 1) throw new InternalProcessingError("Variable " + d + " has been found multiple times, it doesn't match data flow specification, " + toAggregateList)
          Variable.unsecure(d, merging.head.value) :: acc
        }
        else acc
    }
  }

}
