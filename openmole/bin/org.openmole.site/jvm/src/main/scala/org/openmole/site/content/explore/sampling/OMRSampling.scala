package org.openmole.site.content.explore.sampling

/*
 * Copyright (C) 2026 Romain Reuillon
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


object OMRSampling extends PageContent(html"""
${h2{"Load sampling from an OMR file"}}

You can reload some result data and use them as a sampling using the ${code("OMRSampling")}:

${
  hl.openmole("""
    val i = Val[Int]
    val d = Val[Double]

    val o = Val[Int]

    // Define the sampling by specifying the OMR File and the array variables you want to load
    val mySampling = OMRSampling(workDirectory / "file.omr", Seq(i, d)))

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
s""")}

The OMR file must contain the variable i and d, and they must respectively ${code("Array[Int]")} and ${code("Array[Double]")}.
  """)

