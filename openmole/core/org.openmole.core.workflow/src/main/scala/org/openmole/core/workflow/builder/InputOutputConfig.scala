/**
 * Created by Romain Reuillon on 26/05/16.
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
 *
 */
package org.openmole.core.workflow.builder

import monocle.macros.Lenses
import org.openmole.core.context.{ PrototypeSet, Val }
import org.openmole.core.workflow.tools.DefaultSet

object InputOutputConfig {
  def apply(
    inputs:   PrototypeSet = PrototypeSet.empty,
    outputs:  PrototypeSet = PrototypeSet.empty,
    defaults: DefaultSet   = DefaultSet.empty
  ): InputOutputConfig = new InputOutputConfig(inputs, outputs, defaults)

  implicit class ConfigDecoration(config: InputOutputConfig) {
    def add(inputs: Seq[Val[_]] = Seq.empty, outputs: Seq[Val[_]] = Seq.empty) = InputOutputConfig.add(inputs, outputs)(config)
    def addInput(input: Val[_]*) = add(inputs = input)
    def addOutput(output: Val[_]*) = add(outputs = output)
  }

  def add(inputs: Seq[Val[_]] = Seq.empty, outputs: Seq[Val[_]] = Seq.empty) =
    InputOutputConfig.inputs.modify(_ ++ outputs) andThen
      InputOutputConfig.outputs.modify(_ ++ outputs)
}

@Lenses case class InputOutputConfig(
  inputs:   PrototypeSet,
  outputs:  PrototypeSet,
  defaults: DefaultSet
)

import monocle.Lens
import org.openmole.core.context.PrototypeSet
import org.openmole.core.workflow.task.ClosureTask
import org.openmole.core.workflow.tools.DefaultSet

object InputOutputBuilder {

  def apply[T](taskInfo: Lens[T, InputOutputConfig]) = new InputOutputBuilder[T] {
    override def inputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.inputs
    override def defaults: Lens[T, DefaultSet] = taskInfo composeLens InputOutputConfig.defaults
    override def outputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.outputs
  }

}

trait InputOutputBuilder[T] extends InputBuilder[T] with OutputBuilder[T] with DefaultBuilder[T]

