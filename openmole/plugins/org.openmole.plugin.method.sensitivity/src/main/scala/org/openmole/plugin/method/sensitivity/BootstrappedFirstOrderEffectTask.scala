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
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Context._
import SaltelliSampling._
import SensitivityTask._
import org.openmole.core.model.task.IPluginSet

object BootstrappedFirstOrderEffectTask {

  def apply(
    name: String,
    modelInputs: Iterable[IPrototype[Double]],
    modelOutputs: Iterable[IPrototype[Double]],
    bootstrap: Int)(implicit plugins: IPluginSet) = new BootstrappedFirstOrderEffectTaskBuilder(name, SaltelliSampling.matrixName, modelInputs, modelOutputs, bootstrap)

  def apply(
    name: String,
    matrixName: IPrototype[String],
    modelInputs: Iterable[IPrototype[Double]],
    modelOutputs: Iterable[IPrototype[Double]],
    bootstrap: Int)(implicit plugins: IPluginSet) = new BootstrappedFirstOrderEffectTaskBuilder(name, matrixName, modelInputs, modelOutputs, bootstrap)

  class BootstrappedFirstOrderEffectTaskBuilder(
      val name: String,
      val matrixName: IPrototype[String],
      val modelInputs: Iterable[IPrototype[Double]],
      val modelOutputs: Iterable[IPrototype[Double]],
      val bootstrap: Int)(implicit plugins: IPluginSet) extends BootstrappedSensitivityTask.Builder { builder ⇒

    def toTask = new BootstrappedFirstOrderEffectTask(name, matrixName, modelInputs, modelOutputs) {
      val inputs: IDataSet = builder.inputs
      val outputs: IDataSet = builder.outputs
      val parameters = builder.parameters
      val bootstrap = builder.bootstrap
    }

  }

}

abstract sealed class BootstrappedFirstOrderEffectTask(
  val name: String,
  val matrixName: IPrototype[String],
  val modelInputs: Iterable[IPrototype[Double]],
  val modelOutputs: Iterable[IPrototype[Double]])(implicit val plugins: IPluginSet) extends BootstrappedSensitivityTask with FirstOrderEffect
