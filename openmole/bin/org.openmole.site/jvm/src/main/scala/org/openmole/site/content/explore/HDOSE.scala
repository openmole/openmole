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

The High Dimension Origin Space Exploration (HDOSE) method is used to ${b{"explore the multiple antecedents of a pattern"}} when the input space is of high dimension (i.e. > 6). It generates input parameter values that produce a pattern described by a set of constraints on the objectives (i.e. objectives should be under given thresholds or close to given values). HDOSE optimize the fitness and keeps and archives the solutions that are good enough. New solutions are added to the archive when their input values are distant of at least a distance ${i("d = 1.0")} of all existing solution in the archive (the distance L1 is used). When the archive size reaches 500 a new value for ${i("d")} is computed to shrink the archive size under 500 given that all the solutions present in the archive are distant of at least ${i("d")}.


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


${i{"origin"}} describes the genome of the optimisation. To compute the distance, each part of the genome are normalised. To give more weight to a given part of the genome in the distance computation, use the `weight` keyword.

$br

${i{"objective"}} use inequalities to define the pattern to reach. The pattern is reached when all the objectives are under their threshold value. In this example, HDOSE computes a maximal diversity of inputs for which all the outputs are under their respective threshold value.

""")
