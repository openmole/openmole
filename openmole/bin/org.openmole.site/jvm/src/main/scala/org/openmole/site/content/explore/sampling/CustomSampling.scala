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

object CustomSampling extends PageContent(html"""
${h2{"Write your own sampling"}}

You can define a custom sampling in a CSV file and inject it in OpenMOLE.
The provided CSV file must be formatted according to the following template:

$br$br

${hl("""
colD, i
0.7,  8
0.9,  19
0.8,  19
""", "plain")}

${h2{"Use your custom sampling in OpenMOLE"}}

The ${code{"CSVSampling"}} task is used to import your custom sampling into OpenMOLE.
Here is an example of how to use this task in a simple workflow:

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

""")
