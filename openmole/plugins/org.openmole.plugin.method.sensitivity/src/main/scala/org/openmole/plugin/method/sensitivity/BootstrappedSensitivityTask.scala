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

package org.openmole.plugin.method.sensitivity

import java.util.Random
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._

import Task._
import SaltelliSampling._
import SensitivityTask._

object BootstrappedSensitivityTask {

  abstract class Builder extends SensitivityTask.Builder {
    override def outputs: IDataSet = super.outputs + DataSet(for (i ← modelInputs; o ← modelOutputs) yield indice(name, i, o).toArray)
  }

  def bootstrapMatrix(m: Seq[Double])(implicit rng: Random) =
    (0 until m.size).map { i ⇒ m(rng.nextInt(m.size)) }

}

import BootstrappedSensitivityTask._

trait BootstrappedSensitivityTask extends SensitivityTask {

  def bootstrap: Int

  override def process(context: IContext): IContext = {
    implicit val rng = buildRNG(context)
    val matrixNames = context.valueOrException(matrixName.toArray)

    Context.empty ++
      (for (i ← modelInputs; o ← modelOutputs) yield {
        val (a, b, c) = extractValues(context.valueOrException(o.toArray), matrixNames, i)
        new Variable(
          indice(name, i, o).toArray,
          bootstraped(a, b, c).toArray)
      })
  }

  def bootstraped(a: Seq[Double], b: Seq[Double], c: Seq[Double])(implicit rng: Random): Seq[Double] =
    (0 until bootstrap).map {
      i ⇒
        computeSensitivity(
          bootstrapMatrix(a),
          bootstrapMatrix(b),
          bootstrapMatrix(c))
    }

}

