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

object PPSE extends PageContent(html"""

${h2{"PPSE"}}

The PPSE method is a variant of PSE that computes the likelihood of a pattern given probability distributions over the input space. PPSE efficiently discovers the diversity of patterns produced by a simulation model and estimates the marginal likelihood of all the discovered patterns.

$br$br

${h2{"Example"}}

Here is an example of using the PPSE method in an OpenMOLE script:

$br$br

${hl.openmole("""
val param1 = Val[Double]
val param2 = Val[Double]
val output1 = Val[Double]
val output2 = Val[Double]

// PPSE method
PPSEEvolution(
  evaluation = modelTask,
  parallelism = 10,
  termination = 100,
  genome = Seq(
    param1 in (0.0 to 1.0),
    param2 in (-10.0 to 10.0)),
  objective = Seq(
    output1 in (0.0 to 40.0 by 5.0),
    output2 in (0.0 to 4000.0 by 50.0))
) hook (workDirectory / "results", frequency = 100)
""", name = "PPSE", header = "val modelTask = EmptyTask()")}

Optionally, you can define density distributions on your inputs:

$br$br

${hl.openmole("""
val param1 = Val[Double]
val param2 = Val[Double]
val output1 = Val[Double]
val output2 = Val[Double]

// PPSE method with density distributions
PPSEEvolution(
  evaluation = modelTask,
  parallelism = 10,
  termination = 100,
  genome = Seq(
    param1 in (0.0 to 1.0),
    param2 in (-10.0 to 10.0)),
  objective = Seq(
    output1 in (0.0 to 40.0 by 5.0),
    output2 in (0.0 to 4000.0 by 50.0)),
  density = Seq(
    param1 in NormalDistribution(2.0, 0.1),
    param2 in NormalDistribution(0.0, 1.0)
  )
) hook (workDirectory / "results", frequency = 100)
""", name = "PPSE density", header = "val modelTask = EmptyTask()")}

""")



