package org.openmole.site.content.advancedConcepts.GA

/*
 * Copyright (C) 2025 Romain Reuillon
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

object Suggestion extends PageContent(html"""

In OpenMOLE evolutionary algorithms you can suggest genomes to start with. You can suggest genomes using an OMR file, a CSV file or setting it manually. It might be a way to inject use the results found a previous run to bootstrap a new one.

$br

In the following examples we demonstrate suggestion using NSGA2, but it works with any evolutionary method.

${h2("From an OMR file")}

If you saved the results of an evolution in a file called ${i("previous_result.omr")}, you can start an evolution using these results using suggestion. The file must contain values for each parts of the genome.

${
  hl.openmole(s"""
    val i1 = Val[Double]
    val i2 = Val[Double]

    val fitness = Val[Double]

    val rastrigin = ScalaTask($tq
      val i = Vector(i1, i2)
      val fitness = 10 * i.size + i.map(x => (x * x) - 10 * math.cos(2 * Pi * x)).sum
      $tq) set (
        inputs += (i1, i2),
        outputs += fitness
      )


    NSGA2Evolution(
      evaluation = rastrigin,
      objective = fitness,
      genome = Seq(
        i1 in (-5.12 to 5.12),
        i2 in (-5.12 to 5.12)
      ),
      termination = 100,
      parallelism = 10,
      suggestion = workDirectory / "previous_result.omr"
    ) hook (workDirectory / "result.omr")
  """,  name = "OMR Suggestion")
}


${h2("From an CSV file")}

You can also suggest genome values using a CSV file. The file must contain values for each parts of the genome, for instance:
${
  hl.code(
    """
      |i1,i2
      |-1.79,-0.95
      |-2.79,-0.10
      |""".stripMargin)
}

You can suggest the genome listed in this file as follows:
${
  hl.openmole(s"""
    val i1 = Val[Double]
    val i2 = Val[Double]

    val fitness = Val[Double]

    val rastrigin = ScalaTask($tq
      val i = Vector(i1, i2)
      val fitness = 10 * i.size + i.map(x => (x * x) - 10 * math.cos(2 * Pi * x)).sum
      $tq) set (
        inputs += (i1, i2),
        outputs += fitness
      )


    NSGA2Evolution(
      evaluation = rastrigin,
      objective = fitness,
      genome = Seq(
        i1 in (-5.12 to 5.12),
        i2 in (-5.12 to 5.12)
      ),
      termination = 100,
      parallelism = 10,
      suggestion = workDirectory / "suggestion.csv"
    ) hook (workDirectory / "result.omr")
  """,  name = "OMR Suggestion")
}


${h2("Suggest in the script")}

You can also suggest initial genomes in the script, using the following syntax:
${
  hl.openmole(
    s"""
       |
       |val i1 = Val[Double]
       |val i2 = Val[Double]
       |
       |val fitness = Val[Double]
       |
       |val rastrigin = ScalaTask($tq
       |  val i = Vector(i1, i2)
       |  val fitness = 10 * i.size + i.map(x => (x * x) - 10 * math.cos(2 * Pi * x)).sum
       |  $tq) set (
       |    inputs += (i1, i2),
       |    outputs += fitness
       |  )
       |
       |
       |NSGA2Evolution(
       |  evaluation = rastrigin,
       |  objective = fitness,
       |  genome = Seq(
       |    i1 in (-5.12 to 5.12),
       |    i2 in (-5.12 to 5.12)
       |  ),
       |  termination = 100,
       |  parallelism = 10,
       |  suggestion = Seq(
       |    Suggestion(i1 := 1.4, i2 := 1.3),
       |    Suggestion(i1 := -1.3, i2 := -1.2)
       |  )
       |) hook (workDirectory / "file.omr")""".stripMargin)
}

""")