package org.openmole.site.content.advancedConcepts.GA

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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._


object IslandValue {

  def model = """
    // model inputs
    val x = Val[Double]
    val y = Val[Double]

    // model outputs
    val o1 = Val[Double]
    val o2 = Val[Double]

    val model = ScalaTask("val o1 = x; val o2 = y") set (
      inputs += (x, y),
      outputs += (o1, o2)
    )
  """

}

import IslandValue.*

object Island extends PageContent(html"""

${h2{"Distribution scheme"}}

For distributed environments, the island distribution scheme of evolutionary algorithms is especially well adapted.
Islands of population evolve for a while on a remote node before being merged back into the global population.
A new island is then generated until the termination criterion, ${i{"i.e."}} the max total number of individual evaluation, has been reached.

$br

The island scheme is enabled using the ${code{"by Island"}} syntax.
For instance:

${hl.openmole("""
  // Generate a workflow that orchestrates 100 concurrent islands.
  // The workflow stops when 10,000 individuals have been evaluated.
  val evolution =
    NSGA2Evolution(
      genome = Seq(x in (0.0, 1.0), y in (0.0, 1.0)),
      objective = Seq(o1, o2),
      evaluation = model,
      termination = 10000,
      parallelism = 100
    ) by Island(5 minutes) hook (workDirectory / "evolution")

  // Construction of the complete mole with the execution environment, and the hook.
  // Here the generated workflow will run using 4 threads of the local machine.
  (evolution on LocalEnvironment(4))
""", header = model)}

""")
