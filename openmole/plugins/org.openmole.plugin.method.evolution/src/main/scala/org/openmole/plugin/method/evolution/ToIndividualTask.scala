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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.GAFitness._
import fr.iscpif.mgo.ga.GAGenome
import fr.iscpif.mgo.ga.GAIndividual
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet

object ToIndividualTask {
  
  def apply[T <: GAGenome](name: String, genome: IPrototype[T], individual: IPrototype[Individual[T, GAFitness]])(implicit plugins: IPluginSet) = 
    new TaskBuilder { builder =>
      
      var _objectives: List[(IPrototype[Double], Double)] = Nil
      
      def objectives = new {
        def +=(p: IPrototype[Double], v: Double) = {
          inputs += p
          _objectives ::= (p -> v)
          builder
        }
      }
      
      def toTask = new ToIndividualTask[T](name, genome, individual) {
        val inputs = builder.inputs + genome
        val outputs = builder.outputs + individual
        val parameters = builder.parameters
        val objectives = _objectives.reverse
      }
    }
  
}

sealed abstract class ToIndividualTask[T <: GAGenome](
  val name: String, 
  genome: IPrototype[T], 
  individual: IPrototype[Individual[T, GAFitness]])(implicit val plugins: IPluginSet) extends Task { task =>
  
  
  def objectives: List[(IPrototype[Double], Double)]
  
  override def process(context: IContext) = 
    context + new Variable(
      individual, 
      new GAIndividual[T, GAFitness] {
        val genome = context.valueOrException(task.genome)
        val fitness =  new GAFitness {
          val values = objectives.reverse.map {
            case (o, v) => math.abs(context.valueOrException(o) - v)
          }.toIndexedSeq
        }
      }
    )
  
  
}
