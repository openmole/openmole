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

package org.openmole.core.workflow.task

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer

object ExplorationTask {
  type SampledValues = Iterable[Iterable[Variable[_]]]

  def apply(sampling: Sampling) = {
    new TaskBuilder { builder ⇒

      addInput(sampling.inputs.toSeq: _*)
      addOutput(sampling.prototypes.map { p ⇒ Data(p, Explore).toArray }.toSeq: _*)

      def toTask =
        new ExplorationTask(sampling) with builder.Built

    }
  }

}

abstract class ExplorationTask(val sampling: Sampling) extends Task {

  //If input prototype as the same name as the output it is erased
  override protected def process(context: Context) = {
    val variablesValues = TreeMap.empty[Prototype[_], ArrayBuffer[Any]] ++ sampling.prototypes.map { p ⇒ p -> ArrayBuffer[Any]() }
    val rng = newRNG(context)

    for {
      sample ← sampling.build(context)(rng)
      v ← sample
    } {
      variablesValues.get(v.prototype) match {
        case Some(b) ⇒ b += v.value
        case None    ⇒
      }
    }

    context ++ variablesValues.map {
      case (k, v) ⇒
        try Variable(k.toArray.asInstanceOf[Prototype[Array[_]]],
          v.toArray(k.`type`.asInstanceOf[Manifest[Any]]))
        catch {
          case e: ArrayStoreException ⇒ throw new UserBadDataError("Cannot fill factor values in " + k.toArray + ", values " + v)
        }
    }
  }

}
