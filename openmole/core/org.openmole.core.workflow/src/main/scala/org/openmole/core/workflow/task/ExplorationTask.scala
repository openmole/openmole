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

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole.Capsule
import org.openmole.core.workflow.sampling._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import cats.implicits._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.tools._

object ExplorationTask {

  def apply(sampling: Sampling)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    FromContextTask("ExplorationTask") { p ⇒
      import p._

      val variablesValues = TreeMap.empty[Val[_], ArrayBuffer[Any]] ++ sampling.prototypes.map { p ⇒ p → p.`type`.manifest.newArrayBuilder().asInstanceOf[collection.mutable.ArrayBuilder[Any]] }

      for {
        sample ← sampling().from(context)
        v ← sample
      } variablesValues.get(v.prototype) match {
        case Some(b) ⇒ b += v.value
        case None    ⇒
      }

      variablesValues.map {
        case (k, v) ⇒
          try {
            Variable.unsecure(
              k.toArray,
              v.result
            )
          }
          catch {
            case e: ArrayStoreException ⇒ throw new UserBadDataError("Cannot fill factor values in " + k.toArray + ", values " + v)
          }
      }: Context
    } set (
      inputs += (sampling.inputs.toSeq: _*),
      exploredOutputs += (sampling.prototypes.toSeq.map(_.toArray): _*)
    )

  def explored(c: Capsule) = (p: Val[_]) ⇒ c.task.outputs.explored(p)

}
