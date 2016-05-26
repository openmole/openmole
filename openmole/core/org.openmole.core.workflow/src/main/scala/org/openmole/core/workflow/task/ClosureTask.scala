/**
 * Created by Romain Reuillon on 06/05/16.
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
package org.openmole.core.workflow.task

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._

import scala.util.Random

object ClosureTask {

  def taskBuilder[T](closureTask: Lens[T, ClosureTask]) = new TaskBuilder[T] {
    override def inputs: Lens[T, PrototypeSet] = closureTask composeLens ClosureTask.inputs
    override def defaults: Lens[T, DefaultSet] = closureTask composeLens ClosureTask.defaults
    override def name: Lens[T, Option[String]] = closureTask composeLens ClosureTask.name
    override def outputs: Lens[T, PrototypeSet] = closureTask composeLens ClosureTask.outputs
  }

  implicit def isBuilder = taskBuilder(Lens.id)

  def apply(closure: (Context, RandomProvider) ⇒ Context, className: String = "ClosureTask") =
    new ClosureTask(
      closure,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None,
      className = className
    )

}

@Lenses case class ClosureTask(
    closure:                (Context, RandomProvider) ⇒ Context,
    inputs:                 PrototypeSet,
    outputs:                PrototypeSet,
    defaults:               DefaultSet,
    name:                   Option[String],
    override val className: String
) extends Task {
  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context =
    closure(context, rng)
}
