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

object Profile extends PageContent(html"""

${h2{"Profile parameters"}}

The profile (or calibration profile) method is designed to test the sensitivity of the input parameters in a calibration context.
Calibration profile algorithm differs from traditional sensitivity analysis: it captures the full effect of a parameter variation on the model fitness, ${b{"every other input being calibrated"}} to optimize the calibration criterion.

$br$br

In the following we will use the term of ${b{"fitness"}} to denote the calibration error function, or calibration criterion.
Several types of evaluation functions can be used as fitness.
Most of the time, they are either some kind of distance between model dynamics and data, or statistical measures performed on the output dynamics.


${h3{"Method's score"}}

${Resource.rawFrag(Resource.img.method.profileID)}

$br

The calibration profile method is suited to reveal a model's sensitivity regarding its parameter, hence the highest score possible in sensitivity.
However, it does not retrieve information about the input space nor the output space structures, as it focus on ${b{"one"}} parameter/input, every other input being let free (to be calibrated).
As the studied parameter varies, the other parameter are calibrated, so this method scores very well
regarding calibration.
For the same reason, it can handle stochasticity.
Finally, the profile method realizes calibrations on the other inputs for each interval of the input under study, so the more inputs, the more sensitivity to dimensionality of input space.

$br

${Resource.rawFrag(Resource.img.method.profileAnim)}

$br

${basicButton("Run", classIs(btn, btn_danger))(id := shared.profile.button, svgRunButton(-70))}

$br

Given a fitness function, the profile of a selected parameter ${b{"i"}} is constructed by dividing its interval into subintervals of equal size.
For each subinterval, ${b{"i"}} is fixed, and the model is calibrated to minimise the fitness, similarly to ${a("Calibration", href := DocumentationPages.calibration.file)}.
The optimisation is performed over the other parameters of the model.

$br$br

As an example, let's consider a model with 3 parameters ${b{"i"}}, ${b{"j"}} and ${b{"k"}}, each taking real values between 1 and 10.
The profile of the parameter ${b{"i"}} is made by splitting the [1,10] interval into 9 intervals of size 1.
Then, calibration is performed in parallel within each subinterval of ${b{"i"}}.

$br

At the end of the calibration, we obtain sets of ${b{"i"}},${b{"j"}} and ${b{"k"}} values minimising the fitness, with ${b{"i"}} taking values in each subinterval.
By plotting the fitness against the values of ${b{"i"}}, one can visually determine if the model, within each of ${b{"i"}}  subintervals, is able to produce acceptable dynamics.

$br

An important nuance is that for each point of this fitness vs ${b{"i"}} values plot, fitness values has been obtained by "adjusting" ${b{"j"}} and ${b{"k"}} to counterbalance the effect of ${b{"i"}}'s values on the model dynamics.
This means that ${b{"j"}} and ${b{"k"}} values vary for each point of the ${b{"i"}} parameter profile plot.



${h2{"Profile within OpenMOLE"}}
${h3{"Specific constructor"}}

The ${code{"ProfileEvolution"}} constructor takes the following arguments:

${ul(
  li{html"${hl.code("evaluation")}: the model task, that has to be previously declared in your script,"},
  li{html"${hl.code("objective")}: the fitness, a sequence of output variable defined in the OpenMOLE script that is used to evaluate the model dynamics, to be minimized (for this method it is advised to use a single objective), "},
  li{html"${hl.code("profile")}: the sequence of parameter to profile, use in key word to set the number of intervals (${i{"i.e.g"}} Seq(myParam) or Seq(myParam in 100) or Seq(myParam in (1.0 to 5.0 by 0.5))),"},
  li{html"${hl.code("genome")}: a list of the model input parameters with their values interval declared with the ${hl.code("in")} operator,"},
  li{html"${hl.code("termination")}: a \"quantity\" of model evaluation allocated to the profile task. Can be given in number of evaluations (${i{"e.g."}} 20000) or in computation time units (${i{"e.g."}} 230 minutes, 2 days...),"},
  li{html"${hl.code("parallelism")}: $optional number of parallel calibrations performed within ${b{"i"}} subinterval, defaults to 1,"},
  li{html"${hl.code("stochastic")}: $optional the seed provider, mandatory if your model contains randomness,"},
  li{html"${hl.code("distribution")}: $optional computation distribution strategy, default is \"SteadyState\"."},
  li{html"${code{"reject"}}: $optional a predicate which is true for genomes that must be rejected by the genome sampler (for instance \"i1 > 50\")."}
)}

${h3{"Hook"}}

The output of the genetic algorithm must be captured with a hook which saves the current optimal population.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/directory\""}} to save the results as CSV files in a specific directory, or ${code{"hook display"}} to display the results in the standard output.

$br

The hook arguments for the ${code{"ProfileEvolution"}} are:
${Evolution.hookOptions}

For more details about hooks, check the corresponding ${aa("Language", href := DocumentationPages.hook.file)} page.


${h3{"Use example"}}

To build a profile exploration, use the ${code{"ProfileEvolution"}} constructor.
Here is an example :

$br$br

${hl.openmole("""
  val param1 = Val[Double]
  val param2 = Val[Double]
  val fitness = Val[Double]
  val mySeed = Val[Long]

  ProfileEvolution(
    evaluation = modelTask,
    objective = fitness,
    profile = param1,
    genome = Seq(
        param1 in (0.0, 99.0),
        param2 in (0.0, 99.0)
    ),
    termination = 2000000,
    parallelism = 500,
    stochastic = Stochastic(seed = mySeed, sample = 100)
  ) by Island(10 minutes) hook(workDirectory / "path/to/a/directory")
""", header = "val modelTask = EmptyTask()", name = "Profile")}



${h2{"Interpretation guide"}}

A calibration profile is a 2D plot with the value of the profiled parameter represented on the X-axis and the best possible calibration error (${i{"i.e."}} fitness value) on the Y-axis.
To ease the interpretation of the profiles, we define an acceptance threshold on the calibration error (${i{"i.e."}} fitness).
Under this acceptance threshold, the calibration error is considered sufficiently satisfying and the dynamics exposed by the model sufficiently acceptable.
Over this acceptance threshold the calibration error is considered too high and the dynamics exposed by the model are considered unacceptable, or at least non-pertinent.

$br

The computed calibration profiles may take very diverse shapes depending on the effect of the parameter of the model dynamics, however some of this shapes are recurrent.
The most typical shapes are shown on the figure bellow.

$br$br
${img(src := Resource.img.method.profileInterpretation.file, width := "70%")}
$br

${ul(
  li{html"Shape 1 occurs when a parameter is constraining the dynamic of the model (with respect to the fitness calibration criterion). The model is able to produce acceptable dynamics only for a ${i{"single"}} specific range of the parameter. In this case a ${i{"connected"}} (i.e. \"one piece\" domain) validity interval can be established for the parameter."},
  li{html"Shape 2 occurs when a parameter is constraining the dynamic of the model (with respect to the fitness calibration criterion), but the validity domain of the parameter is not connected. It might mean that several qualitatively different dynamics of the model meet the fitness requirement. In this case, model dynamics should be observed directly to determine if every kind of dynamics is suitable or if the fitness should be revised."},
  li{html"Shape 3 occurs when the model \"resists\" to calibration. The profile does not expose any acceptable dynamic according to the calibration criterion. In this case, the model should be improved or the calibration criterion should be revised."},
  li{html"Shape 4 occurs when a parameter does not constrain sufficiently the model dynamics (with respect to the fitness calibration criterion). The model can always be calibrated whatever the value of the profiled parameter. In this case this parameter constitute a superfluous degree of liberty for the model : its effect is compensated by a variation of the other parameters. In general it means that either this parameter should be fixed, or that a mechanism of the model should be removed, or that the model should be reduced by expressing the value of this parameter as a function of the other parameters."}
)}
The calibration profile algorithm has been published in the following paper:

Romain Reuillon, Clara Schmitt, Ricardo De Aldama, and Jean-Baptiste Mouret, «A New Method to Evaluate Simulation
       Models: The Calibration Profile (CP) Algorithm » published in ${i{"Journal of Artificial Societies and Social Simulation"}}
        (JASSS) , Vol 18, Issue 1, 2015.
        $br
        ${a("[online version]" , href:= shared.link.paper.jassCP)}  ${a("[bibteX]", href:= Resource.bibtex.profilemethodBib.file)}


${h3{"Stochastic models"}}

${a("You can check additional options to profile stochastic models on this page.", href := DocumentationPages.stochasticityManagement.file)}

""")
