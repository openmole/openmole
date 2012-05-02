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

package org.openmole.plugin.method.sensitivity

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IPrototype
import SaltelliSampling._

object SensitivityTask {
  def indice(name: String, input: IPrototype[Double], output: IPrototype[Double]) = new Prototype[Double](name + input.name.capitalize + output.name.capitalize)

  abstract class Builder extends TaskBuilder {
    val name: String
    val matrixName: IPrototype[String]
    val modelInputs: Iterable[IPrototype[Double]]
    val modelOutputs: Iterable[IPrototype[Double]]

    override def inputs: IDataSet = super.inputs + DataSet(modelInputs.map(_.toArray)) + DataSet(modelOutputs.map(_.toArray)) + matrixName.toArray
    override def outputs: IDataSet = super.outputs + DataSet(for (i ← modelInputs; o ← modelOutputs) yield indice(name, i, o))
  }
}

import SensitivityTask._

trait SensitivityTask extends Task {

  def matrixName: IPrototype[String]
  def modelInputs: Iterable[IPrototype[Double]]
  def modelOutputs: Iterable[IPrototype[Double]]

  override def process(context: IContext): IContext = {
    val matrixNames = context.valueOrException(matrixName.toArray)

    Context.empty ++
      (for (i ← modelInputs; o ← modelOutputs) yield new Variable(indice(name, i, o), computeSensitivity(context.valueOrException(o.toArray), matrixNames, i)))
  }

  def computeSensitivity(allValues: Array[Double], allNames: Array[String], input: IPrototype[Double]): Double

}