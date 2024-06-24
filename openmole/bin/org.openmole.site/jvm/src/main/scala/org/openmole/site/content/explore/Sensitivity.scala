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

object Sensitivity extends PageContent(html"""


${h2{"Sensitivity analysis"}}

Sensitivity analysis correspond to a set of methods capturing how a model reacts to a change in its inputs.
The goal of these statistical methods is to measure how variation propagates from the inputs to the outputs.
More specifically, sensitivity analysis is defined by (Saltelli et al., 2008) as describing the «relative importance of each input in determining [output] variability».
As a consequence, typical result of such methods is an ordering of its inputs according to their sensitivity.

$br

Sensitivity analysis generally involve an a priori sampling of the input space and a statistical method to analyse the co-variance of the inputs and outputs of the model.

$br$br

Sensitivity analysis can be done at a global or local level.
Global methods provide summary statistics of the effects of inputs variation in the complete input space, whereas local methods focus the effect of inputs variation around a given point of the input space (think of a Jacobian matrix e.g.).
The ${aa("one factor at a time method", href := DocumentationPages.elementarySamplings.file)} can be viewed as a local sensitivity method, as only one factor vary, the other remaining fixed at their nominal value.

$br

OpenMOLE implements two classical methods for global sensitivity analysis: Morris and Saltelli.



${h2{"Morris method"}}
${h3{"Principle"}}

${aa("Morris method", href := "https://en.wikipedia.org/wiki/Morris_method")} is a statistical method for global sensitivity analysis.
This method is of the type "one-factor-at-a-time", and was conceived as a preliminary computational experiment, to grasp the relative influence of each factor.
In comparison to LHS screening, it has the advantage to provide information for each factor.

The input space is considered as a grid and trajectories are sampled among these points.
The method captures output variation when one of the trajectory points is moved to one of its closest neighbors.
This variation is called an elementary effect.
A certain number of trajectories ${b{"R"}} are generated, in order to observe the consequence of elementary effects anywhere in the input space
 (trajectories are generated such that given a starting point, any point at fixed distance is equiprobable - note that the method is still subject
 to the curse of dimensionality for trajectories to fill the input space).
Finally, the method summarizes these elementary effect to estimate global sensitivity in the output space.

This method is computationally cheap, as each trajectory has ${b{"k+1"}} parameter points if ${b{"k"}} is the number of factors.
The total number of model runs will thus be ${b{"R*(k+1)"}}, so the number of trajectory can be adjusted to the computational budget.

${h3{"Results and Interpretation"}}

Morris' method computes three sensitivity indicators for each model input and each model output.
An elementary effect for input ${b{"i"}} and output ${b{indice("y", "j")}} is obtained when the factor ${b{indice("x", "i")}} is changed during one trajectory by a step ${b{indice("delta", "i")}} from an point ${b{indice("x", "0")}}, and computed as ${b{indice("epsilon", "ij")}} = (${indice("y", "j")} * ${indice("x", "0")} - ${indice("y", "j")} * (${indice("x", "0")} + ${indice("delta", "i")}) / ${indice("delta", "i")}).
These are computed as summary statistics on simulated elementary effects and are:

${ul(
  li{html"""The overall sensitivity measure, ${b{indice("i", "j")}} is the average of the elementary effects ${b{indice("epsilon", "ij")}}, computed on effects for which factor ${b{"i"}} was changed (by construction there are exactly ${b{"R"}} such effects, one for each trajectory, and they are independent).
        It is interpreted as the average influence of the input ${b{"i"}} on the model output ${b{"j"}} variability. Note that it can aggregate very different strengths of effects, and more dramatically will cancel opposite effects: an indicator which profile along a dimension is a squared function for example will be considered as unsensitive to the input regarding this sensitivity index."""},
  li{html"A more robust version of the final sensitivity measure, ${b{html"mu${sup{"*"}}${sub{"ij"}}"}}, is computed as the average of the absolute value of the elementary effects, ensuring robustness against non-monotonic models. It is still an average and will miss non-linear effects."},
  li{html"""To account for non-linearities or interactions between factors, the measure ${b{indice("sigma", "ij")}} is computed as the standard deviation of the elementary effects.
      A low standard deviation means that effects are constant, i.e. the output is linear on this factor. A high value will mean either that the indicator is non-linear on this factor (low variations at some places and high variations are others) or that variations change a lot when changing other factor values, i.e. that this factor has interactions with others. Both are equivalent regarding a projection on the dimension of factor considered."""}
)}


${h2{"Morris' method within OpenMOLE"}}
${h3{"Specific constructor"}}

The ${code{"SensitivityMorris"}} constructor is defined in OpenMOLE and takes the following parameters:

${ul(
  li{html"${code{"evaluation"}} is the task (or a composition of tasks) that uses your inputs, typically your model task."},
  li{html"${code{"inputs"}} is the list of your model's inputs."},
  li{html"${code{"outputs"}} is the list of your model's outputs, which behavior is evaluated by the method."},
  li{html"${code{"sample"}} is the number of trajectories sampled, which in practice will determine the accuracy of estimation of sensitivity indices but also the total number of runs."},
  li{html"${code{"levels"}} is the resolution of relative variations, i.e. the number of steps ${b{"p"}} for each dimension of the grid in which trajectories are sampled. In other words, any variation of a factor ${b{indice("delta","i")}} will be a multiple of ${b{"1/p"}}. It should be adapted to the number of trajectories: a higher number of levels will be more suited to a high number of trajectories, to not miss parts of the space with a small number of local trajectories."}
)}

${h3{"Use example"}}

Here is how you can make use of this constructor in OpenMOLE:

$br$br

${hl.openmole("""
val i1 = Val[Double]
val i2 = Val[Double]
val i3 = Val[Double]

val o1 = Val[Double]
val o2 = Val[Double]

SensitivityMorris(
  evaluation = model,
  inputs = Seq(
    i1 in (0.0, 1.0),
    i2 in (0.0, 1.0),
    i3 in (0.0, 1.0)),
  outputs = Seq(o1, o2),
  sample = 10,
  level = 10
) hook display
""", header = "val model = EmptyTask()", name = "Morris")}


${h3{"Additional material"}}

Paper describing method and its evaluation:  ${a("Campolongo F, Saltelli A, Cariboni, J, 2011, From screening to quantitative sensitivity analysis. A unified approach, Computer Physics Communication. 182 4, pp. 978-988." , href := "http://www.andreasaltelli.eu/file/repository/Screening_CPC_2011.pdf")}

The book on sensitivity analysis is also a good reference for the description of sensitivity analysis methods and case studies of their applications: Saltelli, A., Tarantola, S., Campolongo, F., & Ratto, M. (2004). Sensitivity analysis in practice: a guide to assessing scientific models (Vol. 1). New York: Wiley.

$br

${a("OpenMOLE Market example", href:="https://github.com/openmole/openmole-market")}


${h2{"Saltelli's method"}}

Saltelli is a statistical method for global sensitivity analysis. It estimates sensitivity indices based on relative variances.
More precisely, the first order sensitivity coefficient for factor ${b{indice("x", "i")}} and output indicator ${b{indice("y", "j")}} is computed by first conditionally to any ${b{indice("x", "i")}} value, estimating the expectancy of ${b{indice("y", "j")}} conditionally to the value of ${b{indice("x", "i")}} with all other factors varying, and then considering the variance of these local conditional expectancies.
In simpler words, it is the variance after projecting along the dimension of the factor.
It is written as 


${b(html"${indice("Var", "~i")}}[${indice("E", indice("X","i"))}(${indice("y", "j")} | ${indice("x", "i")})] / Var[${indice("y", "j")}]} where ${b{indice("X", "~i")}} are all other factors but ${b{indice("x", "i")}}")}.

An other global sensitivity index does not consider a projection but the full behavior along the factor for all other possible parameter values.
This corresponds to the total effect, i.e. first order but also interactions with other factors.



This index is written as ${b(html"${indice("E", indice("X", "~i"))}[${indice("Var", indice("x", "i"))}(${indice("y", "j")}|${indice("X", "~i")} / Var[${indice("y", "j")}]")}.

In practice, Sobol quasi-random sequences are used to estimate the indices.
The computational budget for this method is fixed by the number of Sobol points drawn, so in practice the user controls directly the number of model runs.

${h2{"Saltelli's method within OpenMOLE"}}
${h3{"Specific constructor"}}

The ${code{"SensitivitySaltelli"}} constructor is defined in OpenMOLE and can take the following parameters:

${ul(
  li{html"${code{"evaluation"}} is the task (or a composition of tasks) that uses your inputs, typically your model task."},
  li{html"${code{"inputs"}} is the list of your model's inputs"},
  li{html"${code{"outputs"}} is the list of your model's outputs for which the sensitivity indices will be computed."},
  li{html"${code{"samples"}} number of samples to draw for the estimation of the relative variances, which correspond exactly to the number of model runs. The higher the dimension, the poorer the estimation of indices will be for low number of samples."}
)}

${h3{"Hook"}}

The @code{hook} keyword is used to save or display results generated during the execution of a workflow.
The generic way to use it is to write either ${code("hook(workDirectory / \"path/of/a/file\")")} to save the results, or ${code{"hook display"}} to display the results in the standard output.

In the output file contains for each index 2 parts: the ${code{"firstOrderIndices"}} and the ${code{"totalOrderIndices"}}, which contain each the matrices of indices for each factor and each indicator.

${h3{"Use example"}}

Here is how you can make use of this constructor in OpenMOLE:

$br$br

${hl.openmole("""
val i1 = Val[Double]
val i2 = Val[Double]
val i3 = Val[Double]

val o1 = Val[Double]
val o2 = Val[Double]

SensitivitySaltelli(
  evaluation = model,
  inputs = Seq(
    i1 in (0.0, 1.0),
    i2 in (0.0, 1.0),
    i3 in (0.0, 1.0)),
  outputs = Seq(o1, o2),
  sample = 100
) hook display
""", header = "val model = EmptyTask()", name = "Saltelli")}


${h3{"Additional material"}}

The variance-based Saltelli method is well described in the paper: Saltelli, A., Annoni, P., Azzini, I., Campolongo, F., Ratto, M., & Tarantola, S. (2010). Variance based sensitivity analysis of model output. Design and estimator for the total sensitivity index. Computer Physics Communications, 181(2), 259-270.

Methods for checking the convergence of indices have been introduced in the literature (see e.g. Sarrazin, F., Pianosi, F., & Wagener, T. (2016). Global Sensitivity Analysis of environmental models: Convergence and validation. Environmental Modelling & Software, 79, 135-152.), and will soon be introduced in OpenMOLE.


""")
