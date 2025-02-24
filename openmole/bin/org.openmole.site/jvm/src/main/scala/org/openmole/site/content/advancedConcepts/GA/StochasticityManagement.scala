package org.openmole.site.content.advancedConcepts.GA

/*
 * Copyright (C) 2023 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.site.content.header.*

object StochasticityManagementValue {

  def model = """
  // model inputs
  val x = Val[Double]
  val y = Val[Double]

  // model outputs
  val o1 = Val[Double]
  val o2 = Val[Double]

  val model = ScalaTask("val o1 = x; val o2 = y") set (
    inputs += (x, y),
    outputs += (o1, o2)
  )
  """


}

import StochasticityManagementValue.*

object StochasticityManagement extends PageContent(html"""

${h2{"OpenMOLE's strategy"}}

Calibration of stochastic models leads to noisy fitness functions that may jeopardize Genetic Algorithms (GA) convergence.
An efficient strategy to deal with such fitness functions is implemented in OpenMOLE.
This strategy automatically balances the need for replications and the discovery of new solutions.

$br

In case you want to explore a stochastic model with a GA you can do:

${hl.openmole(s"""
  $model
  val mySeed = Val[Long]

  val evolution =
    NSGA2Evolution(
      genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)), // genome (of individuals) is the inputs prototype and their variation ranges
      objective = Seq(o1, o2), // objective sets the objectives to minimise
      // OpenMOLE provides a seed for your stochastic model to use (it is optional)
      // 100 replication are stored at max for each individual
      stochastic = Stochastic(seed = mySeed, sample = 100),
      evaluation = model,
      termination = 100
    )
""")}


${h2{"The problem of stochasticity in model calibration"}}

GAs don’t cope well with stochasticity.
This is especially the case for algorithms with evolution strategies of type "µ + λ" (such as NSGA-II, the GA used in OpenMOLE), which preserve the best solutions (individuals) from a generation to another.
In that kind of optimization, the quality of a solution is only ${b{"estimated"}}.
Since it is subject to variations from a replication to another, the quality can either be overvalued or undervalued, ${i{"i.e."}} estimated at a significantly greater or lower value than the one obtained for an infinite number of replications.

$br

Undervalued solutions are not that problematic.
They might be discarded instead of being kept, but the algorithm has a chance to retry a very similar solution later on.
On the other hand, the overvalued solutions are very problematic: the GA will keep them in the population of good solutions because they have been (wrongly) evaluated as such, and will generate new offspring solutions from them.
This behaviour can greatly slow down the convergence of the calibration algorithm, and even make it converge toward sets of parameters producing very unstable dynamics, very likely to produce false good solutions.


${h3{"Existing solutions"}}
To reduce the influence of the fitness fluctuations, the simplest approach is "resampling".
It consists in replicating the fitness evaluation of individuals.
The computed "quality" of an individual is then an estimation (${i{"e.g."}} mean or median) based on a ${i{"finite"}} number of replications of the fitness computation.

$br

This number is set to a compromise between the computation time taken to evaluate one set of parameters (an individual), and an acceptable level of noise for the computed quality.
Still, any number of replications, even very high, implies that some solutions are overvalued with a non negligible probability, given that the fitness function is evaluated millions of times.

$br

Other ${i{"ad hoc"}} methods of the literature are based on some assumptions that are hard or impossible to verify (such as the invariance of the noise distribution over the fitness space) and add parameters to the algorithm that are difficult to tune finely.
See ${aa("this paper", href:= Resource.literature.rakshit2016.file)} for an extensive review of noisy fitness management in Evolutionary Computation.


${h3{"OpenMOLE's solution"}}
To overcome these limitations, OpenMOLE uses an auto-adaptive strategy called "stochastic resampling".
The idea is to  evaluate individuals with only one replication and, at the next generation, to keep and re-inject a sample of the individuals of the current population in the newly created population.

$br

For instance, for each generation, 90% of the individual offspring genomes are ${b{"new genomes"}} obtained by classical mutation/crossover steps of genetic algorithms, and 10% of the offspring genomes are drawn randomly from the current population ${i{"i.e."}} ${b{"already evaluated genomes"}}, for which the algorithm computes one additional replication.
Replicated evaluations are stored for each individual in a vector of replications.
The global fitness of an individual is computed using (for instance) the median of each fitness value stored in the replication vector.

$br

This evolution strategy intends to have the best individuals survive several generations and therefore be the most likely to be resampled, since each individual has a fixed chance of being resampled for each generation.
However, this fixed probability of resampling is not sufficient alone, since well evaluated solutions are likely to be replaced by overvalued solutions (new solutions with a few "lucky" replications).
So as to compensate this bias, we add a technical objective to NSGA2: maximize the number of evaluations of a solution.

$br$br

The optimization problem of model calibration becomes a ${b{"multi-objective optimisation problem"}} (if it was not already !): the algorithm has to optimize the objectives of the model ${b{"and"}} the technical objective as well.
Therefore, the number of replications is taken into account in the Pareto compromise elitism of NSGA-II: solutions with many replications are kept, even if some solutions are better on the other objectives but have been evaluated fewer times.
By doing so, we let the multi-objective optimization algorithm handle the compromise between the quality of the solutions, and their robustness.

$br

This method adds only two new parameters:
    ${ol(
      li{html"""${code{"reevaluate"}}, $optional, the probability of resampling an individual at each generation, the default is 0.1,"""},
      li{html"${code{"sample"}}, the maximum number of evaluation values for an individual, to limit the memory used to store an individual."}
    )}

See the line ${code{"stochastic = Stochastic(seed = mySeed, reevaluate = 0.2, sample = 100)"}} in the example.

$br

This method has been implemented in the library for evolutionary computing: MGO, and is currently being benchmarked against other stochasticity management methods (see ${aa("the repository", href := shared.link.repo.mgobench)}).

""")
