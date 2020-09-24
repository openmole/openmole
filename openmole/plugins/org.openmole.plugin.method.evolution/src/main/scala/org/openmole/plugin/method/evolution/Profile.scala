///*
// * Copyright (C) 2014 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.plugin.method.evolution
//
//import org.openmole.core.dsl._
//import cats._
//import org.openmole.core.context.Context
//import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
//import org.openmole.core.workflow.tools.DefaultSet
//import org.openmole.plugin.method.evolution.Genome.Suggestion
//
//object Profile {
//
//  def apply(
//    x:          Val[Double],
//    nX:         Int                          = 100,
//    genome:     Genome,
//    objective:  Objective[_],
//    stochastic: OptionalArgument[Stochastic] = None,
//    reject:     OptionalArgument[Condition]  = None,
//    nicheSize:  OptionalArgument[Int]        = None
//  ) = {
//    stochastic.option match {
//      case None ⇒
//        NichedNSGA2(
//          Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
//          genome,
//          objective = Seq(objective),
//          nicheSize = nicheSize.option.getOrElse(1),
//          reject = reject
//        )
//      case Some(stochastic) ⇒
//        NichedNSGA2(
//          Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
//          genome,
//          Seq(objective),
//          nicheSize.option.getOrElse(10),
//          stochastic = stochastic,
//          reject = reject
//        )
//    }
//  }
//
//}
//
//object ProfileEvolution {
//
//  import org.openmole.core.dsl._
//
//  def apply(
//    x:            Val[Double],
//    nX:           Int                          = 100,
//    genome:       Genome,
//    objective:    Objective[_],
//    evaluation:   DSL,
//    termination:  OMTermination,
//    nicheSize:    OptionalArgument[Int]        = None,
//    stochastic:   OptionalArgument[Stochastic] = None,
//    reject:       OptionalArgument[Condition]  = None,
//    parallelism:  Int                          = 1,
//    distribution: EvolutionPattern             = SteadyState(),
//    suggestion:   Suggestion                   = Suggestion.empty,
//    scope:        DefinitionScope              = "profile") =
//    EvolutionPattern.build(
//      algorithm =
//        Profile(
//          x = x,
//          nX = nX,
//          genome = genome,
//          objective = objective,
//          stochastic = stochastic,
//          nicheSize = nicheSize,
//          reject = reject
//        ),
//      evaluation = evaluation,
//      termination = termination,
//      parallelism = parallelism,
//      distribution = distribution,
//      suggestion = suggestion(genome),
//      scope = scope
//    )
//
//}
