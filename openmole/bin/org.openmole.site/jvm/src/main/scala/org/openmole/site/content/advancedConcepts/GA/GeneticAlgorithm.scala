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

import org.openmole.site.content.header.*

object GeneticAlgorithmValue {
  def model = s"""
    // model inputs
    val x = Val[Double]
    val y = Val[Double]
    val s = Val[String]

    // model outputs
    val o1 = Val[Double]
    val o2 = Val[Double]

    val model = ScalaTask($tq
        val o1 = x
        val o2 = s match {
        case "apple" => y
        case "banana" => -y
        case "strawberry" => -2 * y
        }$tq) set (
        inputs += (x, y, s),
        outputs += (o1, o2)
        )
    """
}

import GeneticAlgorithmValue.*

object GeneticAlgorithm extends PageContent(html"""

The various ${a("methods", href := DocumentationPages.explore.file)} available in OpenMOLE make an extensive use of genetic algorithms (GA).
For instance, it is the case for the @aa("model calibration method", href := calibration.file), which is an optimization problem, or the search for output diversity with the @a("PSE method", href:= pse.file), which boils down to a GA with a novelty incentive.

$br

GAs can be smartly distributed on grid environments using an ${a("island scheme", href := DocumentationPages.island.file)}, and are able to deal with ${a("stochastic models", href:= DocumentationPages.stochasticityManagement.file)}.


${h2{"About Calibration and GA"}}

OpenMOLE provides advanced methods to help you calibrate your model.
These methods automatically generate workflows to explore the parameter space of your model towards the "best" parameter set, according to a previously defined ${b{"criterion"}} or ${b{"objective"}}.
This is commonly addressed in the literature as a calibration, or optimization, problem.

$br

The different calibration methods in OpenMOLE use GAs to explore the parameter space of a simulation model, looking for parameter sets that will produce outputs reaching one or several given objectives.
@b{Objectives functions}, also called ${b{"fitness functions"}}, compute quantities from the model outputs that have to be minimized or maximized.
They are a quantification of the ${i{"optimal model output"}} you are looking for.

$br

A common optimization problem is data fitting.
In this particular case, the objective function could compute the distance between simulation results and data points, a classical example is the Squared Error function.
If you want your model to reproduce several characteristics (sometimes called stylised facts), you need several objectives, each of them quantifying the similarity between your model outputs and the characteristics you want to reproduce.

$br

To calibrate your model, you need to define:
 ${ul(
    li(html"""
        the ${b{"genome"}} of your model, ${i{"i.e."}} the parameters to be calibrated. They are the dimensions of the parameter
         space that will be explored by the GA. The GA will try different genomes, and keep the best one discovered yet.
    """),
    li(html"the ${b{"objectives"}} you want to reach, expressed as variables to be ${b{"minimized"}}."),
    li(html"a ${b{"termination criterion"}}, to stop the method eventually.")

 )}

${h2{"Dummy Model Optimization Example"}}

This workflow optimizes a dummy model using the generational NSGA-II multi-objective algorithm.
You can replace the instances of ${code{"model"}} by your own model, and adapt the variation range of the input variables.
If you are not familiar with parameter tuning using GA, you should first consult the ${a("tutorial", href := DocumentationPages.netLogoGA.file)} explaining how to calibrate a NetLogo model with a GA.

${hl.openmole(s"""
$model
// Construction of the workflow orchestrating the genetic algorithm
// genome is the inputs prototype and their variation ranges
// objective sets the objectives to minimize
// termination is the termination criterion, here it runs for 100 generations. A time limit could be set as an
// alternative by replacing 100 by 1 hour (hour is a duration type understood by OpenMOLE).
// the parallelism specifies how many evaluation are concurrently submitted to the execution environment
val evolution =
  NSGA2Evolution(
    genome = Seq(
      x in (0.0, 1.0),
      y in (0.0, 1.0),
      s in List("apple", "banana", "strawberry")),
    objective = Seq(o1, o2),
    evaluation = model,
    parallelism = 10,
    termination = 100
  )

// Construction of the complete workflow with the execution environment, and the hook.
// A hook is attached to save the population of solutions to  workDirectory /evolution on the local machine running OpenMOLE
// Here the generated workflow will run using 4 threads of the local machine.
evolution hook (workDirectory / "evolution") on LocalEnvironment(4)
""", name = "nsga2 example")}

Note that the objectives are given as a sequence of model outputs variables to be @b{minimized}.
So if you want to reach specific target values, like Pi and 42, you can use the @code{delta} keyword:

${hl.openmole(s"""
$model

NSGA2Evolution(
  genome = Seq(
    x in (0.0, 1.0),
    y in (0.0, 1.0),
    s in List("apple", "banana", "strawberry")),
  objective = Seq(o1 delta math.Pi, o2 delta 42),
  evaluation = model,
  parallelism = 10,
  termination = 100
) hook (workDirectory / "evolution")""", name = "nsga2 delta example")}

NB: in this case the results in the saved file will be the difference between the outputs of the model and your objectives.

$br

Obviously, maximization problems are performed by taking the opposite of variables as objectives.
You may use a ${code{"-"}} keyword to minimise the opposite of o1 (${i{"i.e."}} maximize o1).

${hl.openmole(s"""
$model

val maximize = ScalaTask("o1 = -o1") set ((inputs, outputs) += (o1, o2))


NSGA2Evolution(
  genome = Seq(
    x in (0.0, 1.0),
    y in (0.0, 1.0),
    s in List("apple", "banana", "strawberry")),
  objective = Seq(-o1, o2),
  evaluation = model,
  parallelism = 10,
  termination = 100) hook (workDirectory / "evolution")
""", name = "nsga2 maximize example")}



${h2{"Outputs of NSGA2Evolution"}}

As an output, the method produces a result file updated at each generation. This file contains the generation number, the values of parameters and the value or the median value of the objectives at each point. When optimizing a stochastic model, it also contain the variable ${code{"evolution$samples"}}, the number of runs of the model used for the evaluation.



${h2{"Real world Example"}}

This ${a("tutorial", href:= DocumentationPages.netLogoGA.file)} exposes how to use Genetic Algorithms to perform optimization on a NetLogo model.

""")
