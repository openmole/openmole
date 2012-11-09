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

package org.openmole.plugin.builder.sensitivity

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.mole._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._
import org.openmole.plugin.method.sensitivity._

object Sensitivity {

  def indice(name: String, input: Prototype[Double], output: Prototype[Double]) = SensitivityTask.indice(name, input, output)

  def bootStrappedSensitivity(
    name: String,
    model: Puzzle,
    samples: Int,
    bootstrap: Int,
    factors: Iterable[Factor[Double, Domain[Double] with Bounds[Double]]],
    outputs: Iterable[Prototype[Double]])(implicit plugins: PluginSet) = {
    val matrixName = Prototype[String](name + "Matrix")
    val sampling = new SaltelliSampling(samples, matrixName, factors.toSeq: _*)
    val explorationCapsule = StrainerCapsule(ExplorationTask(name + "Exploration", sampling))

    val firstOrderTask =
      BootstrappedFirstOrderEffectTask(
        name + "FirstOrder",
        matrixName,
        factors.map { _.prototype },
        outputs,
        bootstrap)

    val firstOrderCapsule = Capsule(firstOrderTask)

    val totalOrderTask =
      BootstrappedTotalOrderEffectTask(
        name + "TotalOrder",
        matrixName,
        factors.map { _.prototype },
        outputs,
        bootstrap)

    val totalOrderCapsule = Capsule(totalOrderTask)

    val aggregateCapsule = StrainerCapsule(EmptyTask(name + "Aggregate"))

    val puzzle = explorationCapsule -< model >- aggregateCapsule -- (firstOrderCapsule, totalOrderCapsule)

    new Puzzle(puzzle) {
      def firtOrderEffect = firstOrderCapsule
      def totalOrderEffect = totalOrderCapsule
    }

  }

}
