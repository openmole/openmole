package org.openmole.site.content.language

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

object ScalaFunction extends PageContent(html"""

Some useful functions are usable anywhere in OpenMOLE where you would use Scala code.
For instance you can use them in:
    ${ul(
        li(html"${a("ScalaTask", href := DocumentationPages.scala.file)} code"),
        li("string expanded by OpenMOLE (${scala code})"),
        li("OpenMOLE scripts.")
    )}


${h2{"Data processing"}}

OpenMOLE provides a useful functions to aggregate data.
Theses functions can be called on array and vectors.
For instance:

${hl.openmole("""
val pi = Val[Double]
val piAvg = Val[Double]

// Define the average task that average several estimation of pi
val average =
  ScalaTask("val piAvg = pi.average") set (
    inputs += pi.toArray,
    outputs += piAvg
)""")}

This task takes place after an exploration and compute the average of many values of pi.
The presently available functions are:
${ul(
  li(html"${code{"def median: Double"}}, compute the median of the vector,"),
  li(html"${code{"def medianAbsoluteDeviation: Double"}}, compute the median absolute deviation of the vector,"),
  li(html"${code{"def average: Double"}}, compute the average of the vector,"),
  li(html"${code{"def meanSquaredError: Double"}}, compute the mean square error of the vector,"),
  li(html"${code{"def rootMeanSquaredError: Double"}}, compute the root of the mean square error of the vector.")
)}


${h2{"Data comparison"}}

OpenMOLE provides useful functions to compare data series.
This function can be called on array and vectors.
For instance:
${ul(
  li(html"${code{"def absoluteDistance(s1: Seq[Double], s2: Seq[Double]): Double"}}, compute the sum of the absolute distance between the respective elements of s1 and s2,"),
  li(html"${code{"def squareDistance(s1: Seq[Double], s2: Seq[Double]): Double"}}, compute the sum of the squared distance between the respective elements of s1 and s2."),
  li(html"${code{"def dynamicTimeWarpingDistance(s1: Seq[Double], s2: Seq[Double]): Double"}}, compute the ${aa(href := "https://en.wikipedia.org/wiki/Dynamic_time_warping", "dynamic time warping distance")} between s1 and s2.")
)}


${h2{"File creation"}}

It might be useful to create files and folders in ScalaTask code.
To do that, use one of the following functions:
${ul(
    li(html"${code{"def newFile(prefix: String, suffix: String): File"}}, this function creates a new file in the OpenMOLE workspace. You may optionally provide a prefix and suffix for the file name. It would generally be called ${code{"newFile()"}}."),
    li(html"${code{"def newDir(prefix: String): File"}}, this function creates a new directory in the OpenMOLE workspace. You may optionally provide a prefix for the directory name. It would generally be called ${code{"newDir()"}}. This function doesn't create the directory."),
    li(html"${code{"def mkDir(prefix: String): File"}}, this function creates a new directory in the OpenMOLE workspace. You may optionally provide a prefix for the directory name. It would generally be called ${code{"mkDir()"}}. This function creates the directory.")
)}


${h2{"Random number generator"}}

In Scala code you may use a properly initialised random generator by calling ${code{"random()"}}.
For instance you may call ${code{"random().nextInt"}}.

$br

It might sometimes be useful to create a new random number generator.
To do that use ${code{"def newRandom(seed: Long): Random"}}.
The seed is optional.
If it is not provided OpenMOLE will take care of the generator initialisation in a sound manner.
It would generally be called ${code{"newRNG()"}}.

${h2{"Scala Code"}}

OpenMOLE at some places in the DSL OpenMOLE make it possible to use the @i{evaluate} keyword. You can generally provide a snippet of scala code to make some computation on the fly.

${hl.openmole("""
val param1 = Val[Double]
val param2 = Val[Double]

val distance = Val[Double]

NSGA2Evolution(
  evaluation = modelTask,
  objective = distance evaluate "distance / 10.0",
  genome = Seq(
    param1 in (0.0, 99.0),
    param2 in (0.0, 99.0)),
  termination = 100
) hook (workDirectory / "path/to/a/directory")
""", header = "val modelTask = EmptyTask()", name = "Scala Code")}

While this is useful in many cases, it is too limiting in case where your function requiers to read data from external files for instance. In theses cases you need to use the ${i{"ScalaCode"}} keyword.

${hl.openmole("""
val myFile = Val[File]

val param1 = Val[Double]
val param2 = Val[Double]

val distance = Val[Double]

val myCode =
  ScalaCode("distance / myFile.content.toDouble") set (
    myFile := workDirectory / "file.txt"
  )

NSGA2Evolution(
  evaluation = modelTask,
  objective = distance evaluate myCode,
  genome = Seq(
    param1 in (0.0, 99.0),
    param2 in (0.0, 99.0)),
  termination = 100
) hook (workDirectory / "path/to/a/directory")
""", header = "val modelTask = EmptyTask()", name = "Scala Code With File")}


${comment("""/* TODO: Document this part
@h2{Technical functions}

  def classLoader[C: Manifest] = manifest[C].erasure.getClassLoader
  def classLoader(a: Any) = a.getClass.getClassLoader

  def withThreadClassLoader[R](classLoader: ClassLoader)(f: => R) =
    org.openmole.tool.thread.withThreadClassLoader(classLoader)(f)
*/
""")}

""")


