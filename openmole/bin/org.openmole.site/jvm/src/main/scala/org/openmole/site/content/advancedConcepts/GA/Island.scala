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

object IslandValue {

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

import IslandValue.*

object Island extends PageContent(html"""

${h2{"Island distribution scheme"}}

The evolutionary algorithms implemented in OpenMOLE supports the island distribution scheme. This scheme make it possible to run multiple model execution on the remote environment and therefore reducing the queuing and initialisation overhead implied by the workload delegation.
$br
In the islands distribution scheme, islands of population evolve for a while on a remote node before being merged back into the global population of your OpenMOLE instance. It means that each node run a small evolution algorithm and the results of each of these evolution is used to evolve the central population.
$br
Each of the island are executed on the remote node unit until a termination criterion. This termination criterion can be either expressed as a total number of model evaluation or an execution time.

$br

The island scheme is enabled using the ${code{"by Island"}} syntax.
For instance:

${hl.openmole("""
  // Generate a workflow that orchestrates 100 concurrent islands.
  // The workflow stops when 10,000 individuals have been evaluated.
  val evolution =
    NSGA2Evolution(
      genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)),
      objective = Seq(o1, o2),
      evaluation = model,
      termination = 100000,
      parallelism = 100
    ) by Island(5 minutes) hook (workDirectory / "evolution")

  // For the sake of simplicity the generated workflow will run using 4 threads of the local machine.
  // Island are generally more useful for remote execution environments
  evolution on LocalEnvironment(4)
""", header = model)}

In the example above a 100 islands are submitted to the environment. Each of these island runs for 5 minutes. Once an island has ended the result is merge in the global population and a new island is submitted. The evolution stops when 100000 model evaluations have been executed within the islands.

$br

Here is the syntax to stop the islands based on a given number of model executions (here 10 execution).

${hl.openmole("""
  val evolution =
    NSGA2Evolution(
      genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)),
      objective = Seq(o1, o2),
      evaluation = model,
      termination = 10000,
      parallelism = 100
    ) by Island(10) hook (workDirectory / "evolution")

  evolution on LocalEnvironment(4)
""", header = model)}


""")
