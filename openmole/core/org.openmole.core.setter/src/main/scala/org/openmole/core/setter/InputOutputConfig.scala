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
package org.openmole.core.setter

import monocle.*
import org.openmole.core.context.{ PrototypeSet, Val }
import org.openmole.core.argument.DefaultSet

object InputOutputConfig {

  implicit class ConfigDecoration(config: InputOutputConfig) {
    def add(inputs: Seq[Val[?]] = Seq.empty, outputs: Seq[Val[?]] = Seq.empty) = InputOutputConfig.add(inputs, outputs)(config)
    def addInput(input: Val[?]*) = add(inputs = input)
    def addOutput(output: Val[?]*) = add(outputs = output)
  }

  /**
   * Operator to add inputs and outputs to a config
   * @param inputs
   * @param outputs
   * @return
   */
  def add(inputs: Seq[Val[?]] = Seq.empty, outputs: Seq[Val[?]] = Seq.empty) =
    Focus[InputOutputConfig](_.inputs).modify(_ ++ outputs) andThen
      Focus[InputOutputConfig](_.outputs).modify(_ ++ outputs)

}

/**
 * Input/Output configuration for tasks, hooks and sources
 *
 * @param inputs prototype set of inputs
 * @param outputs prototype set of outputs
 * @param defaults default values for prototypes
 */
case class InputOutputConfig(
  inputs:   PrototypeSet = PrototypeSet.empty,
  outputs:  PrototypeSet = PrototypeSet.empty,
  defaults: DefaultSet   = DefaultSet.empty
)

