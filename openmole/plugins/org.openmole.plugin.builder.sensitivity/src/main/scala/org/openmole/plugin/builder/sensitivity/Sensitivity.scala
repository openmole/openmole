/*
 * Copyright (C) 2012 reuillon
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

import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.core.implementation.sampling.DiscreteFactor
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.transition.AggregationTransition
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.task.IPluginSet
import org.openmole.plugin.builder.Builder._
import org.openmole.plugin.method.sensitivity.SaltelliSampling
import org.openmole.plugin.method.sensitivity.SensitivityTask


object Sensitivity {
  
  
  def indice(name: String, input: IPrototype[Double], output: IPrototype[Double]) = SensitivityTask.indice(name, input, output)
//  
//  def sensitivity(
//    name: String,
//    model: Puzzle,
//    samples: Int,
//    factors: Iterable[IFactor[Double, IDomain[Double] with IBounded[Double]]],
//    outputs: Iterable[IPrototype[Double]]
//  )(implicit plugins: IPluginSet) = {
//    val matrixName = new Prototype[String](name + "Matrix")
//   
//    val sampling = new SaltelliSampling(samples, matrixName, factors.toSeq:_*)
//    val explorationCapsule = new StrainerCapsule(ExplorationTask(name + "Exploration", sampling))
//   
//    val firstOrderSensitivityTask = 
//      FirstOrderSensitivityTask(
//        name + "FirstOrderSensitivity", 
//        matrixName, 
//        factors.map{_.prototype}, 
//        outputs
//      )
//    
//  }
  
}
