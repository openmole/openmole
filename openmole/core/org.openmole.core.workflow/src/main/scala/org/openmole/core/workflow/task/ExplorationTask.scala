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
import org.openmole.core.workflow.mole.Capsule
import org.openmole.core.workflow.sampling._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer

object ExplorationTask {
  type SampledValues = Iterable[Iterable[Variable[_]]]

  def apply(sampling: Sampling) = {
    new TaskBuilder { builder ⇒

      addInput(sampling.inputs.toSeq: _*)
      addOutput(sampling.prototypes.toSeq.map(_.toArray): _*)

      def toTask =
        new ExplorationTask(sampling) with builder.Built {
          val explored = sampling.prototypes.map(_.name).toSet
          override def outputs = super.outputs.copy(explore = sampling.prototypes.map(_.name).toSet)
        }

    }
  }

  def explored(c: Capsule) = (p: Prototype[_]) ⇒ c.task.outputs.explored(p)

}

abstract class ExplorationTask(val sampling: Sampling) extends Task {

  //If input prototype as the same name as the output it is erased
  override protected def process(context: Context)(implicit rng: RandomProvider) = {
    val variablesValues = TreeMap.empty[Prototype[_], ArrayBuffer[Any]] ++ sampling.prototypes.map { p ⇒ p -> ArrayBuffer[Any]() }

    for {
      sample ← sampling().from(context)
      v ← sample
    } {
      variablesValues.get(v.prototype) match {
        case Some(b) ⇒ b += v.value
        case None    ⇒
      }
    }

    context ++ variablesValues.map {
      case (k, v) ⇒
        try {
          Variable.unsecure(
            k.toArray,
            v.toArray(k.`type`.manifest.asInstanceOf[Manifest[Any]])
          )
        }
        catch {
          case e: ArrayStoreException ⇒ throw new UserBadDataError("Cannot fill factor values in " + k.toArray + ", values " + v)
        }
    }
  }

}
