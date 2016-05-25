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
package org.openmole.plugin.task.scala

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }

import scala.util.Random

object ClosureTask {

  implicit def isBuilder = new TaskBuilder[ClosureTask] {
    override def outputs: Lens[ClosureTask, PrototypeSet] = ClosureTask.outputs
    override def inputs: Lens[ClosureTask, PrototypeSet] = ClosureTask.inputs
    override def name: Lens[ClosureTask, Option[String]] = ClosureTask.name
    override def defaults: Lens[ClosureTask, DefaultSet] = ClosureTask.defaults
  }

  def apply(closure: (Context, ⇒ Random) ⇒ Context) =
    new ClosureTask(
      closure,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    )

}

@Lenses case class ClosureTask(
    closure:  (Context, ⇒ Random) ⇒ Context,
    inputs:   PrototypeSet,
    outputs:  PrototypeSet,
    defaults: DefaultSet,
    name:     Option[String]
) extends Task {
  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context =
    closure(context, rng())
}
