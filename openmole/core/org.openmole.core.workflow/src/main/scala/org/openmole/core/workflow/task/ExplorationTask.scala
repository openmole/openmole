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

import monocle.macros.Lenses
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder
import builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole.Capsule
import org.openmole.core.workflow.sampling._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import monocle.Lens
import org.openmole.core.workflow.dsl
import dsl._
object ExplorationTask {
  type SampledValues = Iterable[Iterable[Variable[_]]]

  implicit def isBuilder = new TaskBuilder[ExplorationTask] {
    override def defaults = ExplorationTask.defaults
    override def inputs = ExplorationTask.inputs
    override def name = ExplorationTask.name
    override def outputs = ExplorationTask.outputs
  }

  def apply(sampling: Sampling): ExplorationTask = {
    val explorationTask = new ExplorationTask(sampling = sampling)

    explorationTask set (
      dsl.inputs += (sampling.inputs.toSeq: _*),
      dsl.exploredOutputs += (sampling.prototypes.toSeq.map(_.toArray): _*)
    )
  }

  def explored(c: Capsule) = (p: Prototype[_]) ⇒ c.task.outputs.explored(p)

}

@Lenses case class ExplorationTask(
    inputs:   PrototypeSet   = PrototypeSet.empty,
    outputs:  PrototypeSet   = PrototypeSet.empty,
    defaults: DefaultSet     = DefaultSet.empty,
    name:     Option[String] = None,
    sampling: Sampling
) extends Task {

  //If input prototype as the same name as the output it is erased
  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val variablesValues = TreeMap.empty[Prototype[_], ArrayBuffer[Any]] ++ sampling.prototypes.map { p ⇒ p → ArrayBuffer[Any]() }

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
