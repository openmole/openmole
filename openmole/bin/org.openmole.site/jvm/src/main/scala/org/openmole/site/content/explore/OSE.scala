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

object OSE extends PageContent(html"""

${h2{"OSE description"}}

The Origin Space Exploration (OSE) method is used to ${b{"explore the multiples antecedents of a pattern"}}. It produces input parameter values that produce a given pattern described by a set of constraints on the objectives (i.e. objectives should be under given thresholds). OSE optimizes the fitness and when it finds solutions that are good enough it keep them and blacklist the part of the inputs space containing these solution. The optimization process keep going in order to find multiple solutions producing the pattern, in all parts of the input space.

The hook arguments for the ${code{"OSEEvolution"}} are:
${Evolution.hookOptions}

${h3{"Example"}}

Here is a use example of the OSE method in an OpenMOLE script:

$br$br

${hl.openmole("""
val myseed = Val[Long]

val param1 = Val[Double]
val param2 = Val[Double]
val output1 = Val[Double]
val output2 = Val[Double]

OSEEvolution(
  evaluation = modelTask,
  parallelism = 10,
  termination = 100,
  origin = Seq(
    param1  in (0.0 to 1.0 by 0.1),
    param2 in (-10.0 to 10.0 by 1.0)),
  objective = Seq(
    output1 under 5.0,
    output2 under 50.0),
  stochastic = Stochastic(seed = myseed)
) hook (workDirectory / "results.omr", frequency = 100)
""", name = "OSE", header = "val modelTask = EmptyTask()")}


${i{"origin"}} describes the discrete space of possible origins. Each cell is considered a potential origin. ${i{"objective"}} describe the pattern to reach with inequalities. The sought patten is considered as reached when all the objective are under their threshold value. In this example OSE computes a maximal diversity of inputs for which all the outputs are under their respective threshold values.

""")
