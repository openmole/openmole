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

package org.openmole.plugin.evolution

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.GAGenome
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.implementation.data.Prototype.toArray
import fr.iscpif.mgo.ga.selection.Ranking
import fr.iscpif.mgo.tools.Scaling._

class ScalingParetoTask[I <: Individual[GAGenome, GAFitness] with Ranking](
  name: String,
  archivePrototype: IPrototype[Array[I]]) extends Task(name) {

  addInput(archivePrototype)
  
  var scaled: List[(IPrototype[Double], Double, Double)] = Nil

  def scale(p: IPrototype[Double], min: Double, max: Double) = {
    scaled ::= ((p, min, max))
    addOutput(toArray(p))
  }
  
  var objectives: List[IPrototype[Double]] = Nil
  
  def objective(p: IPrototype[Double]) = {
    objectives ::= p
    addOutput(toArray(p))
  }
  
  override def process(context: IContext) = {
    val pareto = Ranking.pareto[I](context.valueOrException(archivePrototype))

    (
      scaled.reverse.zipWithIndex.map {  case((p, min, max), i) => new Variable(toArray(p), pareto.map{_.genome.values(i).scale(min, max) }.toArray) } ++
      objectives.reverse.zipWithIndex.map { case(p, i) => new Variable(toArray(p), pareto.map{_.fitness.values(i)}.toArray) }
    ).toContext
  }
  
}
