package org.openmole.site.content.explore

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

object HDOSE extends PageContent(html"""

${h2{"HDOSE description"}}

The High Dimension Origin Space Exploration (HDOSE) method is used to ${b{"explore the multiple antecedents of a pattern"}} when the input space has high dimensionality (i.e., > 6 dimensions). It generates input parameter values that produce a pattern described by a set of constraints on the objectives (i.e., objectives should be either below given thresholds or close to specified target values). HDOSE optimizes the fitness and archives the solutions that are good enough. New solutions are added to the archive when their input values are at least distance ${i("d = 1.0")} apart from all existing solutions in the archive (using L1 distance). When the archive size reaches 500, a new value for ${i("d")} is computed to reduce the archive size to below 500, ensuring that all solutions in the archive remain at least distance ${i("d")} apart.


The hook arguments for the ${code{"HDOSEEvolution"}} are:
${Evolution.hookOptions}

${h3{"Example"}}

This is an example of the HDOSE method in an OpenMOLE script:

$br$br

${hl.openmole("""
val myseed = Val[Long]

val param1 = Val[Array[Double]]
val param2 = Val[Double]
val param3 = Val[Array[Boolean]]
val param4 = Val[Array[Int]]

val output1 = Val[Double]
val output2 = Val[Double]

HDOSEEvolution(
  evaluation = modelTask,
  parallelism = 10,
  termination = 100,
  origin = Seq(
    param1 in Seq.fill(10)(0.0 to 1.0),
    param2 in (-10.0 to 10.0 weight 10.0),
    param3 in Seq.fill(5)(TrueFalse weight 2.0),
    param4 in Seq.fill(10)(0 to 100)
  ),
  objective = Seq(
    output1 under 5.0,
    output2 under 50.0),
  stochastic = Stochastic(seed = myseed)
) hook (workDirectory / "results.omr", frequency = 100)
""", name = "HDOSE", header = "val modelTask = EmptyTask()")}


${i{"origin"}} describes the genome of the optimisation. To compute the distance, each part of the genome is normalised. To give more weight to a given part of the genome in the distance computation, use the `weight` keyword.

$br

${i{"objective"}} uses inequalities to define the pattern to reach. The pattern is reached when all objectives are below their threshold value. In this example, HDOSE finds the maximum diversity of inputs for which all outputs are below their respective thresholds.

""")
