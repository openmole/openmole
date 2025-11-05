package org.openmole.site.content.explore.sampling

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

object UniformSampling extends PageContent(html"""

${h2{"Random Sequence of Number"}}

Samplings can be performed at random within a domain following a uniform distribution, via the ${code{"RandomSequence(size, min, max)"}} command.
This task generates values uniformly distributed values:

$br

In the following example, 100 values are generated at random, uniformly distributed between 0 and 20.

$br$br

${hl.openmole("""
val my_input = Val[Double]
val my_model = EmptyTask() set( (inputs, outputs) += my_input)

val exploration =
  DirectSampling(
    evaluation = my_model hook display,
    sampling= my_input in RandomSequence[Double](max = 20, size = 100)
  )

exploration""", name = "uniform sampling example")}



${h2{"Sample within a skewed uniform distribution"}}

Custom domains can be defined using transformations on the uniform distribution.
For instance in this next example, 100 values uniformly distributed between 0 and 20 are still generated at random, however, each one is then shifted by -10 through the ${code{"map"}} function.
Thus, the sampling will be comprised of 100 values uniformly distributed between -10 and 10.

$br$br

${hl.openmole("""
val my_input = Val[Double]
val my_model = EmptyTask() set( (inputs, outputs) += my_input)

val exploration =
  DirectSampling(
    evaluation = my_model hook display,
    sampling= my_input in RandomSequence[Double](max = 20, size = 100).map(x => x -10)
  )

exploration""", name = "uniform sampling custom example")}

$br

For more information on the @code{map} function, or other transformation functions, see the ${a("Language", href := DocumentationPages.language.file)} section.

""")
