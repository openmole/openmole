/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.context.Variable
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.context._
import org.openmole.core.workflow.builder.DefinitionScope

object GenerateIslandTask {

  def apply[T](algorithm: T, sample: Option[Int], size: Int, untypedOutputPopulation: Val[_])(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)
    val outputPopulation = untypedOutputPopulation.asInstanceOf[Val[t.Pop]]

    ClosureTask("GenerateIslandTask") { (context, rng, _) ⇒
      val p = context(t.populationPrototype)

      import t.integration.iManifest

      def samples =
        if (p.isEmpty) Vector.empty
        else sample match {
          case Some(s) ⇒ rng().shuffle(p.toVector).take(s)
          case None    ⇒ p.toVector
        }

      def populations = Array.fill(size)(t.operations.migrateToIsland(samples).toArray)
      Variable(outputPopulation.toArray, populations)
    } set (
      inputs += t.populationPrototype,
      exploredOutputs += outputPopulation.toArray
    )
  }

}