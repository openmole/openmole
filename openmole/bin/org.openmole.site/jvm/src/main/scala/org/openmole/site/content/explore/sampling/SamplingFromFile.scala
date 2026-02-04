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

object SamplingFromFile extends PageContent(html"""
${h2{"Read a sampling from a CSV file"}}

You can define a custom sampling in a CSV file and inject it in OpenMOLE.
The provided CSV file must be formatted according to the following template:

$br$br

${hl("""
colD, i
0.7,  8
0.9,  19
0.8,  19
""", "plain")}

The ${code{"CSVSampling"}} is used to import your custom sampling into OpenMOLE.
Here is an example of how to use this sampling in a simple workflow:

$br$br

${hl.openmole("""
val i = Val[Int]
val o = Val[Int]
val d = Val[Double]

// Define the sampling by mapping the columns of the CSV file to OpenMOLE variables
// comma ',' is the default separator, but you can specify a different one using
val mySampling = CSVSampling(workDirectory / "file.csv", separator = ',') set (
  outputs += i.mapped,
  outputs += d mapped "colD",

)

// Define the model, here it just takes i as input
val myModel =
  ScalaTask("val o = i * d") set (
    inputs += (i, d),
    outputs += (i, d, o)
  )

// Define the exploration of myModel for various i values sampled in the file
val exploration = DirectSampling(
  evaluation = myModel hook display,
  sampling = mySampling
)

exploration""")}

$br

In this example the column ${b{"i"}} in the CSV file is mapped to the OpenMOLE variable ${code{"i"}} and ${b{"colD"}} is mapped to the OpenMOLE variable ${code{"d"}}.

$br

As a sampling, the ${code{"CSVSampling"}} task can directly be injected in a ${code{"DirectSampling"}} task under the ${code{"sampling"}} parameter.
It will generate a different task for each entry in the file.

${h2{"Read a sampling from an OMR file"}}

Let's take an experiment that save the results in a OMR file:
${
  hl.openmole(s"""
    val a = Val[Int]
    val i = Val[Int]

    val d = Val[Double]

    val myModel =
      ScalaTask($tq
        val i = a * 2
        val d = x / 2.0
      $tq) set(
        inputs += a,
        outputs += (i, d)
      )

    DirectSampling(
      evaluation = myModel,
      sampling = a in (5 to 50 by 5)
    ) hook (workDirectory / "file.omr")
  """)
}



You can reload some result data and use them as a sampling using the ${code("OMRSampling")}:

${
  hl.openmole("""
    val i = Val[Int]
    val d = Val[Double]

    val o = Val[Int]

    // Define the sampling by specifying the OMR File and the array variables you want to load
    val mySampling = OMRSampling(workDirectory / "file.omr", Seq(i, d))

    // Define the model, here it just takes i as input
    val myModel =
      ScalaTask("val o = i * d") set(
        inputs += (i, d),
        outputs += (i, d, o)
      )

    // Define the exploration of myModel for various i values sampled in the file
    DirectSampling(
      evaluation = myModel,
      sampling = mySampling
    ) hook display
""")}

The OMR file must contain the variable i and d, and they must respectively ${code("Array[Int]")} and ${code("Array[Double]")}.

""")
