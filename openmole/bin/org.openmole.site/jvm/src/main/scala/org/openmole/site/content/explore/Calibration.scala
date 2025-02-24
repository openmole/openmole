package org.openmole.site.content.explore

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

object Calibration extends PageContent(html"""

Using Genetic Algorithms (GA), OpenMOLE finds the input set matching one or several criteria: this is called ${b{"calibration"}}.
In practice, calibration is used to target ${b{"one"}} specific scenario or dynamic.
Usually, a fitness function is used to assess the distance between obtained dynamics and your target dynamic.
In case your model is not able to match the target dynamic, the calibration will find the parameterization producing the closest (according to your fitness function) possible dynamic.
For more details on calibration using genetic algorithms, see the ${aa("GA detailed page", href := DocumentationPages.geneticAlgorithm.file)}.


${h2{"Single criterion Calibration"}}
${h3{"Method's score"}}

${Resource.rawFrag(Resource.img.method.GAsingleID)}
$br

The single criterion calibration method is designed to solve an optimization problem, so unsurprisingly it performs well regarding the optimization grade.
Since it is only focused towards discovering the best performing individual (parameter set), this method doesn't provide insights about the model sensitivity regarding its input parameters, as it does not keep full records of the past input samplings leading to the optimal solution.

$br

For the same reason, this method is not intended to cover the entirety of the input and output spaces, and thus does not perform well regarding the input and output exploration grades.
It concentrates the sampling of the input space towards the part which minimizes the fitness, and therefore intentionally neglects the part of the input space leading to high fitness values.
Calibration can handle stochasticity, using a ${aa("specific method", href := DocumentationPages.stochasticityManagement.file)}.

$br

The dimensionality of the model input space is generally not an issue for this method, as it can handle up to a hundred dimensions in some cases.
However, the number of objectives (output dimensionality) should be limited to a maximum of 2 or 3 compromise objectives.

$br$br

${figure(img(src := Resource.img.method.calibrationMono.file, width := "70%"))}

$br

Single criterion calibration answers the following question: for a given target value of the output ${b{"o1"}}, what is(are) the parameter set(s) ${b{"(i, j , k)"}} producing the output value(s) closest to the target?



${h2{"Multi criteria Calibration"}}
${h3{"Method's score"}}

${Resource.rawFrag(Resource.img.method.GAmultiID)}

$br

The multi criteria calibration method slightly differs from the single criterion version.
It suffers the same limitations regarding input and output spaces.
However, since it may reveal a Pareto frontier and the underlying trade-off, it reveals a little bit of the model sensitivity, showing that the model performance regarding a criterion is impacted by its performances regarding the others.
This is not genuine sensitivity as in sensitivity analysis, but still, it outlines a variation of your model outputs, which is not bad after all!

$br$br

${figure(img(src := Resource.img.method.calibrationMulti.file, width := "70%"))}

$br

Multi criteria calibration answers the following question: for a given target pattern ${b{"(o1,o2)"}}, what are the parameter sets ${b{"(i,j)"}} that produce the closest output values to the target pattern ?

$br

Sometimes a Pareto Frontier may appear!


${h3{"Differences between single and multi criteria calibration"}}

Calibration boils down to minimizing a distance measure between the model output and some data.
When there is only one distance measure considered, it is single criterion calibration.
When there are more than one distance that matter, it is multi-criteria calibration.

$br

For example, one may study a prey-predator model and want to find parameter values for which the model reproduces some expected size of both the prey and predator populations.

$br$br

The single criterion case is simpler, because we can always tell which distance is smaller between any two distances.
Thus, we can always select the best set of parameter values.

$br

In the multi criteria case, it may not always be possible to tell which simulation output has the smallest distance to the data.
For example, consider a pair (d1, d2) representing the differences between the model output and the data for the prey population size (d1) and the predator population size (d2).
Two pairs such as (10, 50) and (50, 10) each have one element smaller than the other and one bigger.
There is no natural way to tell which pair represents the smallest distance between the model and the data.
Thus, in the multi-criteria case, we keep all the parameter sets (e.g. {(i1, j1, k1), (i2, j2, k2), ...}) yielding such equivalent distances (e.g. {(d11, d21), (d12, d22), ...}).
The set of all these parameter sets is called the Pareto front.



${h2{"Calibration within OpenMOLE"}}
${h3{"Specific constructor"}}

Single and multi-criteria calibration in OpenMOLE are both done with the NSGA2 algorithm, through the ${code{"NSGA2Evolution"}} constructor.
It takes the following parameters:

${ul(
  li{html"${code{"evaluation"}}: the OpenMOLE task that runs the simulation (${i{"i.e."}} the model),"},
  li{html"${code{"objective"}}: a list of the distance measures (which in the single criterion case will contain only one measure),"},
  li{html"${code{"genome"}}: a list of the model parameters and their respective variation intervals,"},
  li{html"${code{"termination"}}: the total number of evaluations (execution of the task passed to the parameter \"evaluation\") to be executed,"},
  li{html"${code{"parallelism"}}: $optional, the number of simulations that will be run in parallel, defaults to 1,"},
  li{html"${code{"populationSize"}}: $optional, the population size, defaults to 200,"},
  li{html"${code{"stochastic"}}: $optional, the seed provider, mandatory if your model contains randomness,"},
  li{html"${code{"distribution"}}: $optional, computation distribution strategy, default is \"SteadyState\"."},
  li{html"${code{"reject"}}: $optional, a predicate which is true for genomes that must be rejected by the genome sampler (for instance \"i1 > 50\")."}
)}

Where ${code{"param1"}}, ${code{"param2"}}, ${code{"param3"}} and ${code{"param4"}} are inputs of the ${code{"modelTask"}}, and ${code{"distance1"}} and ${code{"distance2"}} are its outputs.

$br$br

More details and advanced notions can be found on the ${aa("GA detailed page", href := DocumentationPages.geneticAlgorithm.file)}.


${h3{"Hook"}}

The output of the genetic algorithm must be captured with a hook which saves the current optimal population.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/file.omr\")"}} to save the results as an OMR file, or ${code{"hook display"}} to display the results in the standard output.

$br

The hook arguments fore the ${code{"NSGA2Evolution"}} are:
${Evolution.hookOptions}

For more details about hooks, check the corresponding ${aa("Language", href := DocumentationPages.hook.file)} page.


${h3{"Example"}}

In your OpenMOLE script, the NSGA2 algorithm scheme is defined like so:

$br$br

${hl.openmole("""
val param1 = Val[Double]
val param2 = Val[Double]
val param3 = Val[Int]
val param4 = Val[String]

val distance1 = Val[Double]
val distance2 = Val[Double]

NSGA2Evolution(
  evaluation = modelTask,
  objective = Seq(distance1, distance2),
  genome = Seq(
    param1 in (0.0, 99.0),
    param2 in (0.0, 99.0),
    param3 in (0, 5),
    param4 in List("apple", "banana", "strawberry")),
  termination = 100,
  parallelism = 10
) hook (workDirectory / "path/to/a/file.omr")
""", header = "val modelTask = EmptyTask()", name = "Calibration")}


${h3{"Calibrating a high number of inputs"}}

If you want to calibrate an important number of parameters you can use arrays directly in the genome. For that you must provide an array of boundaries. In this example, the an array of 100 inputs varying between 0 an 100 an a single double value varying from 1 to 10.

${hl.openmole("""
val param1 = Val[Array[Double]]
val param2 = Val[Double]
val param3 = Val[Boolean]

val distance = Val[Double]

val model = ScalaTask("val distance = param1.sum * param2") set (inputs += (param1, param2), outputs += distance)

NSGA2Evolution(
  evaluation = model ,
  objective = distance,
  genome = Seq(
    param1 in Seq.fill(100)(0.0 to 99.0),
    param2 in (1.0 to 10.0),
    param3 in TrueFalse),
  termination = 100,
  parallelism = 10
) hook display
""")}


${h3{"Stochastic models"}}

You can check additional options to calibrate stochastic models on ${aa("this page", href := DocumentationPages.stochasticityManagement.file)}.
""")
