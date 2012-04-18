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
import fr.iscpif.mgo.ga.GAGenome
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import fr.iscpif.mgo.ga.selection.Ranking
import fr.iscpif.mgo.tools.Scaling._
import org.openmole.core.model.task.IPluginSet
import scala.collection.mutable.ListBuffer

object ScalingParetoTask {
  
  def apply[I <: Individual[GAGenome, GAFitness] with Ranking](name: String, archive: IPrototype[Array[I]])(implicit plugins: IPluginSet) = 
    new TaskBuilder { builder =>
    
      private var _scales = new ListBuffer[(IPrototype[Double], Double, Double)]
      private var _objectives = new ListBuffer[IPrototype[Double]] 
    
      def scales = _scales.toList 
   
      def scale(p: IPrototype[Double], min: Double, max: Double) = {
        _scales += ((p, min, max))
        this addOutput p.toArray
        this
      }
      
      def objectives = _objectives.toList
    
      def objective(p: IPrototype[Double]) = {
        _objectives += p
        this addOutput p.toArray
        this
      }
      
      def toTask = new ScalingParetoTask[I](name, archive) {
        val inputs = builder.inputs + archive
        val outputs = builder.outputs
        val parameters = builder.parameters
        val scales = builder.scales
        val objectives = builder.objectives
      }
    }
    
    
 
  
}

sealed abstract class ScalingParetoTask[I <: Individual[GAGenome, GAFitness] with Ranking](
  val name: String,
  archive: IPrototype[Array[I]]) 
(implicit val plugins: IPluginSet) extends Task {

  def scales: List[(IPrototype[Double], Double, Double)]
  def objectives: List[IPrototype[Double]]
  
  override def process(context: IContext) = {
    val pareto = Ranking.firstRanked[I](context.valueOrException(archive))

    (
      scales.reverse.zipWithIndex.map {  case((p, min, max), i) => new Variable(p.toArray, pareto.map{_.genome.values(i).scale(min, max) }.toArray) } ++
      objectives.reverse.zipWithIndex.map { case(p, i) => new Variable(p.toArray, pareto.map{_.fitness.values(i)}.toArray) }
    ).toContext
  }
  
}
