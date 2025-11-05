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

object AdvancedSampling extends PageContent(html"""

Samplings are tools for exploring a space of parameters.
The term ${i{"parameter"}} is understood in a very broad sense in OpenMOLE as it may concern numbers, files, random streams, images, etc.

$br

Many samplings can be defined in the same OpenMOLE script, and combined with, or modified by, other samplings.
The different options offered via the scripting language in OpenMOLE are presented hereafter.

${h2{"Combine samplings"}}

The ${code{"x"}} combinator enables domain bounds to depend on each other.
Notice how the upper bound of the second parameter depends on the value of the first one in the following example:

$br$br

${hl.openmole("""
  val i = Val[Int]
  val j = Val[Double]

  val explo =
   DirectSampling (
     sampling =
       (i in (0 to 10 by 2)) x
       (j in RangeDomain[Double]("0.0", "2 * i", "0.5")),
     evaluation = myModel
   ) hook display
""", name = "combine sampling", header = "val myModel = EmptyTask()")}


${h3{"Concatenate samplings"}}

While the sampling combination with the cartesian product operator ${code{"x"}} produces a complete sampling, one may want to concatenate two samplings.
The operator ${code{"::"}} does so:

$br$br

${hl.openmole("""
((i in Seq(0, 1)) x (j is 2.0)) ++ ((i is 10) x (j is 666.0))
""", header = "val i = Val[Int]; val j = Val[Double]", name ="concatenate samplings")}

${h2{"Zip samplings"}}

To combine the elements of two samplings by their indices, a zip sampling can be used.
In OpenMOLE, there are three different tasks to perform zip samplings.


${h3{"Simple zip sampling"}}

The keyword ${code{"zip"}} mimics the traditional ${b{"zip"}} operation from functional programming by combining elements from two lists.


${h3{"Zip with index sampling"}}

Again, this is inspired by a common functional programming operation called ${b{"zipWithIndex"}}.
Applying ${b{"zipWithIndex"}} to a list would create a new list of pairs, formed by the elements of the original list and the index of their position in the list.
For instance ${code{"List('A', 'B', 'C') zipWithIndex"}} would return the new list ${code{"List(('A',0), ('B',1), ('C',2))"}}.

$br

In OpenMOLE, the keyword ${code{"withIndex"}} performs a similar operation in the dataflow.
A separate integer variable is associated to the sampling to get access to the indices of every sample of the sampling.

$br$br

The following code snippet gives an example of how to use the first two zip samplings:

$br$br

${hl.openmole("""
  val p1 = Val[Int]
  val p2 = Val[Int]

  val s1 = p1 in (0 to 100) // Code to build sampling 1
  val s2 = p2 in (0 to 100) // Code to build sampling 2

  // Create a sampling by zipping line by line s1 and s2
  val s3 = s1 zip s2

  // Create a sampling containing an id for each experiment in a variable called id
  val id = Val[Int]
  val s4 = s2 withIndex id
""", name = "zip sampling")}


${h3{"Zip with name sampling"}}

This last zip sampling maps the names of the files from a ${code{"FileDomain"}} to a ${code{"String"}} variable in the dataflow.
See the page about ${aa("how to sample over files", href := DocumentationPages.fileSampling.file)} for more details about this.

$br

In the following excerpt, we map the name of a file and print it along to its size.
In OpenMOLE, ${code{"File"}} variables generally don't preserve the name of the file from which it was originally created.
In order to save some output results depending on the input filename, the filename should be transmitted in a variable of type ${code{"String"}}.
When running this snippet however, the file is renamed by the ScalaTask and its name is saved in the ${code{"name"}} variable.

$br$br

${hl.openmole(s"""
    val file = Val[File]
    val name = Val[String]
    val size = Val[Long]

    val t = ScalaTask(${tq}val size = (workDirectory / "file").length${tq}) set (
      inputFiles += (file, "file"),
      inputs += name,
      outputs += (name, size)
    )

    DirectSampling(
      sampling = file in (workDirectory / "dir") withName name,
      evaluation = (t hook display)
    )
  """, name = "zip with name sampling")}

$br

If you need to go through several levels of files you may use a sampling like this one:

$br$br

${hl.openmole("""
  val dir = Val[File]
  val dirName = Val[String]
  val file = Val[File]
  val fileName = Val[String]
  val name = Val[String]
  val size = Val[Long]

  val t = ScalaTask("val size = file.length") set (
    inputs += file,
    outputs += size,
    (inputs, outputs) += (fileName, dirName)
  )

  DirectSampling(
    sampling =
      (dir in (workDirectory / "test") withName dirName) x
      (file in dir withName fileName),
    evaluation = t hook display
  )
""", name = "multilevel file sampling")}



${h2{"Create a new sampling from an existing one"}}

OpenMOLE offers different possibilities to create new samplings from an initial sampling, by applying some modifying functions such as ${code{"take"}}, ${code{"filter"}}, or ${code{"sample"}}.

${ul(
  li{html"${code{"firstSampling take N"}}, with @code{N} an integer, will generate a new sampling from the first ${code{"N"}} values of the firstSampling,"},
  li{html"${code{"firstSampling sample N"}} will generate a new sampling from ${code{"N"}} values picked at random from firstSampling,"},
  li{html"${code{"firstSampling filter (\"predicate\")"}} will filter out all the values from firstSampling for which the given predicate is not ${code{"true"}}."}
)}
See the following for a use example of these samplings:

$br$br

${hl.openmole("""
  val p1 = Val[Int]
  val p2 = Val[Int]

  val s1 = p1 in (0 to 100) // Code to build sampling 1
  val s2 = p2 in (0 to 100) // Code to build sampling 2

  // Create a sampling containing the 10 first values of s1
  val s3 = s1 take 10

  // Sample 5 values from s1
  val s4 = s1 sample 5

  // Create a new sampling containing only the lines of s1 for which the given predicate is true
  val s5 = (s1 x s2) filter ("p1 + p2 < 100")

  // Create a sampling dropping 2 values in s5 and then taking the next 10 values
  val s6 = s5 drop 2 take 10

  // Create a sampling containing the subset 4 of size 10 in s5, i.e. the values with indexes 40 to 49
  val s7 = s5 subset (4, size = 10)
""", name = "sampling modifiers")}



${h2{"Random samplings"}}

OpenMOLE can generate random samplings from an initial sampling using ${code{"shuffle"}}. It will create a new sampling which is a randomly shuffled version of the initial one.
OpenMOLE can also generate a fresh new sampling made of random numbers using ${code{"RandomSequence[T]"}}, with ${code{"T"}} the type of random numbers to be generated.

$br

Check the following script to discover how to use these random-based operations in a workflow:

$br$br

${hl.openmole("""
val p1 = Val[Int]
val p2 = Val[Int]

val s1 = p1 in (0 to 100) // Code to build sampling 1
val s2 = p2 in (0 to 100) // Code to build sampling 2

// Create a sampling containing the values of (s1 x s2) in a random order
val s3 = (s1 x s2).shuffle

// Create a sampling containing 10 values drawn at random in (s1 x s2)
val s4 = (s1 x s2).sample(10)

// Replicate 100 times the sampling s1 and provide seed for each experiment
val seed = Val[Int]
val s5 = s1 x (seed in RandomSequence[Int](size = 100))
""", name = "random sampling")}

${h2{"Higher level samplings"}}

Some sampling combinations generate higher level samplings such as ${code{"repeat"}} and ${code{"bootstrap"}}:

$br$br

${hl.openmole("""
    val i = Val[Int]

    val s1 = i in (0 to 100)

    // Re-sample 10 times s1, the output is an array of array of values
    val s2 = s1 repeat 10

    // Create 10 samples of 5 values from s1, it is equivalent to "s1 sample 5 repeat 10", the output type is an
    // array of array of values
    val s3 = s1 bootstrap (5, 10)
""", name = "high level sampling")}

$br

Here is how such higher level samplings would be used within a workflow:

$br$br

${hl.openmole("""
    // This code computes 10 couples (for f1 and f2) of medians among 5 samples picked at random in f1 x f2
    val p1 = Val[Double]
    val p2 = Val[Double]

    val f1 = p1 in (0.0 to 1.0 by 0.1)
    val f2 = p2 in (0.0 to 1.0 by 0.1)

    val stat = ScalaTask("val p1 = input.p1.median; val p2 = input.p2.median") set (
      inputs += (p1.toArray, p2.toArray),
      outputs += (p1, p2)
    )

    DirectSampling(
      sampling = (f1 x f2) bootstrap (5, 10),
      evaluation = stat hook display
    )
""", name = "bootstrap example sampling")}



${h2{"The is keyword"}}

The ${code{"is"}} keyword can be used to assign a value to a variable in a sampling.
For instance:

$br$br

${hl.openmole("""
val i = Val[Int]
val j = Val[Int]
val k = Val[Int]

DirectSampling(
  sampling =
    (i in (0 until 10)) x
    (j is "i * 2") x
    (k in RangeDomain[Int]("j", "j + 7")),
  evaluation = myModel hook display
)
""", header = "val myModel = EmptyTask()")}

""")
