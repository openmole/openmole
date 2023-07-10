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

object HighDimensionSampling extends PageContent(html"""


${h2{"Specific methods for high dimension spaces"}}

High dimension spaces must be handled via specific methods of the literature, otherwise, cartesian products would be too memory consuming.
OpenMOLE includes two of these methods: ${b{"Sobol Sequence"}} and ${b{"Latin Hypercube Sampling"}}, which can be passed as an argument to the ${code{"DirectSampling"}} task:

${h3{"Methods' score"}}

${Resource.rawFrag(Resource.img.method.sobolLHSID)}

$br

These two methods perform well in terms of input space exploration (which is normal as they were built for that), however, they are superior to uniform or grid samplings, while sharing the same intrinsic limitations.
There is no special way of handling stochasticity of the model, out of standard replications.

$br

These methods are not expensive ${i{"per se"}}, it depends on the magnitude of the input space you want to be covered.



${h2{"Latin Hypercube Sampling"}}

The ${aa("Latin Hypercube Sampling", href := "https://en.wikipedia.org/wiki/Latin_hypercube_sampling")} is a statistical method for generating a near-random sample of parameter values from a multidimensional distribution.
The syntax of the LHS sampling in OpenMOLE is the following:

$br$br

${hl.openmole("""
val i = Val[Double]
val j = Val[Double]
val values = Val[Array[Double]]

val my_LHS_sampling =
    LHS(
      sample = 100, // Number of points of the LHS
      factor = Seq(
        i in (0.0, 10.0),
        j in (0.0, 5.0),
        values in Vector((0.0, 1.0), (0.0, 10.0), (5.0, 9.0)) // Generate part of the LHS sampling inside the array of values
      )
    )
""", name = "lhs sampling in sensitivity")}


${h3{"Use in the DirectSampling method"}}

Once a sampling is defined, you can just add it to a ${code{"DirectSampling"}} method (see ${aa("here", href:= DocumentationPages.samplings.file)} for the description of this method), under the ${code{"sampling"}} argument.
For example, supposing you have already declared inputs, outputs, and a model task called ${code{"myModel"}}, the sampling could be used like this:

$br$br

${hl.openmole("""
    val myExploration = DirectSampling(
      evaluation = myModel ,
      sampling = my_lhs_sampling
    )

    myExploration hook display
""", header = """val myModel = EmptyTask(); val my_lhs_sampling = EmptySampling()""")}


${h2{"Sobol Sequence"}}

A ${aa("Sobol sequence", href := "https://en.wikipedia.org/wiki/Sobol_sequence")} is a quasi-random low-discrepancy sequence.
The syntax of the Sobol sequence sampling in OpenMOLE is the following:

$br$br

${hl.openmole("""
val i = Val[Double]
val j = Val[Double]
val values = Val[Array[Double]]

val my_sobol_sampling =
  SobolSampling(
    sample = 100, // Number of points
    factor = Seq(
      i in (0.0, 10.0),
      j in (0.0, 5.0),
      values in Vector((0.0, 1.0), (0.0, 10.0), (5.0, 9.0)) // Generate part of the Sobol sampling inside the array of values
    )
  )
""", name = "sobol sampling in sensitivity")}


${h3{"Use in the DirectSampling method"}}

Once a sampling is defined, you can just add it to a ${code{"DirectSampling"}} method (see ${aa("here", href:= DocumentationPages.samplings.file)} for the description of this method), under the ${code{"sampling"}} argument.
For example, supposing you have already declared inputs, outputs, and a model task called ${code{"myModel"}}, the sampling could be used like this:

$br$br

${hl.openmole("""
    val myExploration = DirectSampling(
      evaluation = myModel ,
      sampling = my_sobol_sampling
    )

    myExploration hook display
    """, header = """val myModel = EmptyTask(); val my_sobol_sampling = EmptySampling()""")}
    
""")
