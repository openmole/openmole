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

object PSE extends PageContent(html"""

${h2{"PSE description"}}

The Pattern Space Exploration (PSE) method is used to ${b{"explore the output's diversity of a model"}}.
Input parameter values are selected to produce new output values, such that as the exploration progresses, the region of the output space that is covered gets bigger.
PSE reveals the potential of your model: the variety of dynamics it is able to produce, even those you were not investigating in the first place!


${h3{"Method's score"}}

${Resource.rawFrag(Resource.img.method.pseID)}

$br

The PSE method is designed to cover the output space, hence it gets the highest possible score in output exploration.
Since PSE is all about ${i{"covering"}} output space, it gets low scores in optimization and input space exploration.
As the method discovers new patterns in the output space, you can get some insight about the sensitivity of the model by looking at the input values leading to these patterns.
Contrarily to calibration-based methods, PSE is sensitive to the dimensionality of the output space, as it records all the locations that were covered during the exploration.
This can quickly become costly for more than three or four dimensions.

$br

PSE handles stochasticity in the sense that the selected patterns are estimated by the median of several model execution output values.


${h3{"How it works"}}

The PSE method searches for diverse output values.
As with all evolutionary algorithms, PSE generates new individuals through a combination of genetic inheritance from the parent individuals and mutation.
PSE (inspired by ${aa("novelty search", href := shared.link.noveltySearch)} selects for the parents whose output values are rare compared to the rest of the population and to the previous generations.
In order to evaluate the rarity of an output value, PSE discretises the output space, dividing it into cells.
Each time a simulation is run and its output is known, a counter is incremented in the corresponding cell.
PSE preferentially selects the parents whose associated cells have low counters.
By selecting parents with rare output values, we try and increase the chances to produce new individuals with previously unobserved behaviours.

${Resource.rawFrag(Resource.img.method.pseAnim)}

$br

${basicButton("Run", classIs(btn, btn_danger))(id := shared.pse.button, svgRunButton(-50))}



${h2{"PSE within OpenMOLE"}}
${h3{"Specific constructor"}}

The OpenMOLE constructor for PSE is ${code{"PSEEvolution"}}.
It takes the following parameters:

${ul(
  li{html"${code{"evaluation"}} the OpenMOLE task that runs the simulation, ${i{"i.e."}} the model,"},
  li{html"${code{"parallelism"}} the number of simulations that will be run in parallel,"},
  li{html"${code{"termination"}} the total number of evaluations to be executed,"},
  li{html"${code{"genome"}} a list of the model parameters and their respective variation intervals,"},
  li{html"${code{"objective"}} a list of indicators measured for each evaluation of the model within which we search for diversity, with a discretization step,"},
  li{html"${code{"stochastic"}} the seed generator, which generates suitable seeds for the method. Mandatory if your model contains randomness. The generated seed for the model task is transmitted through the variable given as an argument of @code{Stochastic} (here myseed)."},
  li{html"${code{"reject"}}: ($optional) a predicate which is true for genomes that must be rejected by the genome sampler (for instance \"i1 > 50\")."},
)}

${h3{"Hook"}}

The outputs of PSE must be captured with a hook.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/file\")"}} to save the results in a OMR file, or ${code{"hook display"}} to display the results in the standard output.

$br

The hook arguments for the ${code{"PSEEvolution"}} are:
${Evolution.hookOptions}

For more details about hooks, check the corresponding ${aa("Language", href := DocumentationPages.hook.file)} page.


${h3{"Use example"}}

Here is a use example of the PSE method in an OpenMOLE script:

$br$br

${hl.openmole("""
// Seed declaration for random number generation
val myseed = Val[Int]

val param1 = Val[Double]
val param2 = Val[Double]
val output1 = Val[Double]
val output2 = Val[Double]

// PSE method
PSEEvolution(
  evaluation = modelTask,
  parallelism = 10,
  termination = 100,
  genome = Seq(
    param1  in (0.0, 1.0),
    param2 in (-10.0, 10.0)),
  objective = Seq(
    output1 in (0.0 to 40.0 by 5.0),
    output2 in (0.0 to 4000.0 by 50.0)),
  stochastic = Stochastic(seed = myseed)
) hook (workDirectory / "results", frequency = 100)
""", name = "PSE", header = "val modelTask = EmptyTask()")}

$br

Where ${code{"param1"}} and ${code{"param2"}} are inputs of the task running the model, and ${code{"output1"}} and ${code{"output2"}} are outputs of that same task.
The number of inputs and outputs are unlimited.

$br$br

Note that this method is subject to the curse of dimensionality on the output space, meaning that the number of output patterns can grow as a power of the number of output
variables.
With more than just a few output variables, the search space may become so big that the search will take too long to complete and the search results will take more memory than a modern computer can handle.
Restricting the number of output variables to 2 or 3 also facilitates the interpretation of the results, making them easy to visualise.

$br$br

The PSE method is described in the following scientific paper :
$br
Guillaume Chérel, Clémentine Cottineau and Romain Reuillon, « Beyond Corroboration: Strengthening Model Validation by Looking for Unexpected Patterns» published in ${i{"PLOS ONE"}} 10(9), 2015.
$br
${a("[online version]" , href:= shared.link.paper.beyondCorroboration)} ${a("[bibteX]", href:= Resource.bibtex.PSEmethodBib.file)}


${h3{"Stochastic models"}}

You can check additional options to run PSE on stochastic models on ${aa("this page", href := DocumentationPages.stochasticityManagement.file)}.

""")
