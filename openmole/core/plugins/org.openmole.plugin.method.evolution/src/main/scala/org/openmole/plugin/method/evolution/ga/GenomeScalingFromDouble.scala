/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.method.evolution.ga

import org.openmole.core.model.data.{ Variable, Context }
import org.openmole.plugin.method.evolution._
import org.openmole.misc.tools.service.Scaling._

trait GenomeScalingFromDouble <: GenomeScaling {
  def inputs: Inputs[Double]

  def scaled(genome: Seq[Double], context: Context): List[Variable[_]] = scaled(inputs.inputs.toList, genome.toList)

  def scaled(scales: List[Input[Double]], genome: List[Double]): List[Variable[_]] =
    if (scales.isEmpty || genome.isEmpty) List.empty
    else {
      val (variable, tail) =
        scales.head match {
          case Scalar(p, min, max)         ⇒ Variable(p, genome.head.scale(min, max)) -> genome.tail
          case Sequence(p, min, max, size) ⇒ Variable(p, genome.take(size).map(_.scale(min, max)).toArray) -> genome.drop(size)
        }

      variable :: scaled(scales.tail, tail.toList)
    }

}
