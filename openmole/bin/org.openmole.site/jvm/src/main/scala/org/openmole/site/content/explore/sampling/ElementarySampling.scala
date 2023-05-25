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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._

object ElementarySampling extends PageContent(html"""
${h2{"Grid sampling"}}

A grid sampling (also called complete sampling) consists in evaluating every possible combination of the provided input values, for a reasonable number of dimensions and discretisation steps.

${h3{"Method's score"}}

${Resource.rawFrag(Resource.img.method.completeID)}

$br

Grid sampling is a good way of getting a first glimpse at the output space of your model when you don't know anything about your input space structure. However, it will not give you any information ont the structure of the output space, as there is no reason for evenly spaced inputs to lead to evenly spaced outputs.

$br

Grid sampling is hampered by input space dimensionality, as high dimension spaces need a lot of samples to be covered, as well as a lot of memory to store them.


${h3{"Use within OpenMOLE"}}

A grid sampling is declared via the ${code{"DirectSampling"}} constructor, in which the bounds and discretisation steps of each input to vary  are declared:

$br$br

${hl.openmole("""
   val input_i = Val[Int]
   val input_j = Val[Double]
   val output1 = Val[Double]
   val output2 = Val[Double]

   DirectSampling(
     evaluation = my_own_model,
     sampling =
       (input_i in (0 to 10 by 2)) x
       (input_j in (0.0 to 5.0 by 0.5)),
     aggregation = Seq(output1 evaluate median, output2)
   ) hook display
""", header = "val my_own_model = EmptyTask()", name = "syntax of DirectSampling Task")}

$br

with
${ul(
  li{html"${code{"evaluation"}} is the task (or composition of tasks) that uses your inputs, typically your model task and a hook,"},
  li{html"${code{"sampling"}} is the sampling task,"},
  li{html"${code{"aggregation"}} ($optional) ${a(href := aggregationSampling.file, "is some aggregation functions")} to be called on the outputs of your evaluation task. The format is ${code{"variable evaluate function"}}. OpenMOLE provides some aggregation functions to such as: ${code{"median, medianAbsoluteDeviation, average, meanSquaredError, rootMeanSquaredError"}}. If no a variable is listed and no aggregate function is provided, the values are aggregated in a array."}
)}

For Double sequence samplings, a convenient primitive provides logarithmic ranges the following way: ${code{"input_j in LogRangeDomain(min,max,number_of_steps)"}} where the third argument is the number of steps in the range. The syntax can be simplified with the ${code{"logSteps"}} keyword like this:  ${code("input_j in (Range(1e-2,0.1) logSteps 4")}.

The ${code{"hook"}} keyword is used to save or display results generated during the execution of a workflow.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/file.csv\")"}} to save the results in a CSV file, or ${code{"hook display"}} to display the results in the standard output.
See ${aa("this page", href := samplings.file + "#Hook")} for more details about this hook.


${h3{"Use example"}}

Here is a dummy workflow showing the exploration of a Java model, that takes an integer value as input, and generates a string as output:

$br$br

${hl.openmole("""
// Inputs and outputs declaration
val i = Val[Int]
val o = Val[Double]
// Defines the model
val myModel =
  ScalaTask("val o = i * 2") set (
    inputs += i,
    outputs += (i, o)
  )


DirectSampling(
  evaluation = myModel hook display,
  sampling = i in (0 to 10 by 1),
  aggregation = Seq(o evaluate average)
) hook display
""", name="concrete example of direct sampling")}

$br

Some details:

${ul(
 li{html"${code{"myModel"}} is the task that multiply the input by 2,"},
 li{html"the ${code{"evaluation"}} attribute of the ${code{"DirectSampling"}} method is the composition of myModel and a hook,"},
 li{html"the ${code{"aggregation"}} attribute of the ${code{"DirectSampling"}} method is set to computes the average upon the values of o,"},
 li{html"the task declared under the name ${code{"DirectSampling"}} is a DirectSampling task, which means it will generate parallel executions of ${code{"myModel"}}, one for each sample generated by the sampling task."}
)}


${h2{"One factor at a time sampling"}}

In the case of models requiring a long time to run, or for preliminary experiments, one may want to proceed to a sampling similar to the grid sampling, with a reduced number of total runs.
For this, one can vary each factor successively in its domain, the others being fixed to a nominal value.
Note that this type of sampling first will necessarily miss potential interactions between factors, and secondly will explore only a very small fraction of the parameter space.
The computational load in terms of number of model runs will then be only the sum of the sizes of factor domains, instead of their product in the case of a full grid.

For example, with two factors ${b{html"${indice("x", "1")} and ${indice("x", "2")}"}} varying each between 0 and 1 with a step of 0.1, if their nominal value is 0.5, the one factor sampling will take first ${b{html"${indice("x", "1")} = 0, 0.1, ... , 1} while @b{${indice("x", "2")} = 0.5}"}}, and then the contrary.

${h3{"Use within OpenMOLE"}}

The sampling primitive ${code{"OneFactorSampling"}} does so and takes as arguments any number of factors decorated by the keyword ${code{"nominal"}} and the nominal value.

It is used as follows in an example with a ${code{"DirectSampling"}}:

$br$br

${hl.openmole("""
val x1 = Val[Double]
val x2 = Val[Double]
val o = Val[Double]

val myModel = ScalaTask("val o = x1 + x2") set (
    inputs += (x1,x2),
    outputs += (x1,x2, o)
  )

DirectSampling(
  evaluation = myModel hook display,
  sampling = OneFactorSampling(
    (x1 in (0.0 to 1.0 by 0.2)) nominal 0.5,
    (x2 in (0.0 to 1.0 by 0.2)) nominal 0.5
  )
)
""", name="example of one factor at a time")}

$br

The ${code{"hook"}} keyword is used to save or display results generated during the execution of a workflow.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/file.csv\")"}} to save the results in a CSV file, or ${code{"hook display"}} to display the results in the standard output.
See ${aa("this page", href := samplings.file + "#Hook")} for more details about this hook.

""")