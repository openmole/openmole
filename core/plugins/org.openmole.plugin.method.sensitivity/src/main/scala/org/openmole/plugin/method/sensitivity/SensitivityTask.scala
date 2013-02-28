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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.DataSet
import org.openmole.core.model.data.Prototype
import SaltelliSampling._
import org.openmole.core.model.task.PluginSet

object SensitivityTask {
  def indice(name: String, input: Prototype[Double], output: Prototype[Double]) = Prototype[Double](name + input.name.capitalize + output.name.capitalize)

  abstract class Builder(implicit plugins: PluginSet) extends TaskBuilder {
    val name: String
    val matrixName: Prototype[String]
    val modelInputs: Iterable[Prototype[Double]]
    val modelOutputs: Iterable[Prototype[Double]]

    override def inputs: DataSet = super.inputs + DataSet(modelInputs.map(_.toArray)) + DataSet(modelOutputs.map(_.toArray)) + matrixName.toArray
  }
}

trait SensitivityTask extends Task {

  def matrixName: Prototype[String]
  def modelInputs: Iterable[Prototype[Double]]
  def modelOutputs: Iterable[Prototype[Double]]

  def computeSensitivity(a: Seq[Double], b: Seq[Double], c: Seq[Double]): Double

}