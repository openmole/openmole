/*
 * Copyright (C) 2014 Romain Reuillon
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
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import fr.iscpif.mgo.algorithm._
import org.openmole.core.workflow.data.PrototypeType
import org.openmole.core.workflow.tools.TextClosure
import org.openmole.tool.statistics._

object NSGA2 {

  def apply(
    mu: Int,
    inputs: Inputs,
    objectives: Objectives,
    replication: Option[Replication]) = {

    val (_mu, _inputs, _objectives) = (mu, inputs, objectives)

    trait OMNSGA2 <: NSGAII with GAAlgorithm {
      val stateType = PrototypeType[STATE]
      val gType = PrototypeType[G]
      val objectives = _objectives
      val inputs = _inputs
      override def mu: Int = _mu
    }

    replication match {
      case None =>
        new OMNSGA2 with DeterministicGAAlgorithm {
          val populationType = PrototypeType[Pop]
          val individualType = PrototypeType[Ind]
          val fitness = Fitness(_.phenotype)
        }
      case Some(replication) =>
        new OMNSGA2 with StochasticGAAlgorithm {
          override def cloneStrategy = queue(replication.max, replication.reevaluate)

          val populationType = PrototypeType[Pop]
          val individualType = PrototypeType[Ind]
          override def aggregation: Option[Seq[TextClosure[Seq[Double], Double]]] = replication.aggregation
          override implicit def fitness: Fitness[Seq[Double]] = Fitness(i => aggregate(i) ++ Seq(-i.phenotype.history.size.toDouble))
        }
    }

  }




}

