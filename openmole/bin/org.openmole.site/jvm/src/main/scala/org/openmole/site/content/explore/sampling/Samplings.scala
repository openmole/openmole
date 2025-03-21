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

object Samplings extends PageContent(html"""

${h2{"Execute a Single Run"}}
Before exploring you model your model you might want to run it for an single set of inputs. To achieve it, the syntax is the following:


${hl.openmole("""
val input_i = Val[Int]
val input_j = Val[Double]

SingleRun(
  evaluation = my_own_model,
  input = Seq(
    input_i := 10,
    input_j := 10.0)
) hook display
""", header = "val my_own_model = EmptyTask()", name = "syntax of SingleRun method")}

This run the model ${i("my_own_model")} once and display the result.


${h2{"Design of Experiment"}}

Design of Experiment (DoE) is the art of setting up an experimentation.
In a model simulation context, it boils down to declaring the inputs under study (most of the time, they're parameters) and the values they will take, for a batch of several simulations, with the idea of revealing a property of the model (${i{"e.g."}} sensitivity).

$br

Your model inputs can be sampled in the traditional way, by using a ${a("grid (or regular) sampling", href:= DocumentationPages.elementarySamplings.file + "#GridSampling")}, or by ${a("sampling uniformly", href:= DocumentationPages.uniformSampling.file)} inside their respective domains.
For higher dimension input space, specific statistics techniques ensuring low discrepancy like ${a("Latin Hypercube Sampling", href := DocumentationPages.highDimensionSamplings.file + "#LatinHypercubeSampling")} and ${a("SobolSequence", href := DocumentationPages.highDimensionSamplings.file + "#SobolSequence")} are available.

$br

You can also use your own DoE in OpenMOLE, by providing a ${a("CSV file", href := DocumentationPages.customSampling.file)} containing your samples to OpenMOLE.

${h2{"The DirectSampling method"}}

In OpenMOLE, a DoE is set up through the ${code{"DirectSampling"}} constructor.
This constructor will generate a workflow, which is illustrated below.
You may recognize the ${i{"map reduce"}} design pattern, provided that an aggregation operator is defined (otherwise it would just be a ${i{"map"}} :-) )

$br$br

${img(src := Resource.img.method.directSampling.file, width := "50%")}


${h3{"Sampling over several inputs"}}

Samplings can be performed over several inputs domains as well as on ${b{"several input types"}}, using the ${b{"cartesian product"}} operator ${b{"x"}} as follow:

$br$br

${hl.openmole("""
val i = Val[Int]
val j = Val[Double]
val k = Val[String]
val l = Val[Long]
val m = Val[File]
val b = Val[Boolean]

DirectSampling(
  evaluation = myModel,
  sampling =
    (i in (0 to 10 by 2)) x
    (j in (0.0 to 5.0 by 0.5)) x
    (k in List("Leonardo", "Donatello", "Raphaël", "Michelangelo")) x
    (l in (UniformDistribution[Long]() take 10)) x
    (m in (workDirectory / "dir").files.filter(f => f.getName.startsWith("exp") && f.getName.endsWith(".csv"))) x
    (b in TrueFalse)
) hook(workDirectory / "path/of/a/file")
""", header = "val myModel = EmptyTask()", name = "several inputs")}

$br

The ${code{"DirectSampling"}} task executes the model ${b{"myModel"}} for every possible combination of the 5 inputs provided in the ${code{"sampling"}} parameter.
The ${code{"hook"}} provided after the task will save the results of your sampling in a file, see the ${a("next section", href := "#Hook")} for more details about this hook.

$br$br

The arguments of the ${code{"DirectSampling"}} task are the following:

${ul(
    li{html"${code{"evaluation"}} is the task (or a composition of tasks) that uses your inputs, typically your model task,"},
    li{html"${code{"sampling"}} is where you define your DoE, ${i{"i.e."}} the inputs you want varied,"},
    li{html"${code{"aggregation"}} ($optional) is ${a(href := DocumentationPages.aggregationSampling.file, "an aggregation operation")} to be performed on the outputs of your evaluation task."}
)}

The ${code{"l"}} parameter is a uniform sampling of 10 numbers of the Long type, taken in the [Long.MIN_VALUE; Long.MAX_VALUE] domain of the Long native type.
More details can be found ${a("here", href := DocumentationPages.uniformSampling.file)}.

$br

The ${code{"m"}} parameter is a sampling over different files that have been uploaded to the @b{workDirectory}.
The files are explored as items of a list, gathered by the ${code{"files()"}} function and applied on the ${code{"dir"}} directory.
Optionally, this list of files can be filtered with any ${code{"String => Boolean"}} functions such as ${code{"contains(), startswith(), endswith()"}} (see the ${aa("Java Class String Documentation", href:= shared.link.javaString)} for more details.
More information on this sampling type ${a("here", href := DocumentationPages.fileSampling.file)}.


${h3{"Hook"}}

The @code{hook} keyword is used to save or display results generated during the execution of a workflow.
The generic way to use it is to write either @code{hook(workDirectory / "path/of/a/file")} to save the results in a file, or @code{hook display} to display the results in the standard output.

@br@br

There are some arguments specific to the ${code{"DirectSampling"}} method which can be added to the hook:

${ul(
  li{html"${code{"output"}} is to choose what to do with the results as shown above, either a file path or the word @code{display},"},
  li{html"${code{"values = Seq(i, j)"}} specifies which variables from the data flow should be saved or displayed, by default all variables from the dataflow are used,"}
)}
Here is a use example:

$br$br

${hl.openmole("""
val i = Val[Int]
val j = Val[Double]

DirectSampling(
  evaluation = myModel,
  sampling =
    (i in (0 to 10 by 2)) x
    (j in (0.0 to 5.0 by 0.5))
) hook(
  output = display,
  values = Seq(i)
)
""", header = "val myModel = EmptyTask()", name = "hook direct sampling")}

$br

For more details about hooks, check the corresponding ${a("Language", href := DocumentationPages.hook.file)} page.

${h2{"Model replication"}}

If your model is stochastic, you may want to define a replication task to run several replications of the model for the same parameter values.
This is similar to using a uniform distribution sampling on the seed of the model, and OpenMOLE provides a specific constructor for this, the ${code{"Replication"}} task.

$br

The ${code{"Replication"}} sampling is used as follow:

$br$br

${hl.openmole("""
val mySeed = Val[Int]
val i = Val[Int]
val o = Val[Double]

val myModel =
  ScalaTask("val rng = Random(mySeed); val o = i * 2 + 0.1 * rng.nextDouble()") set (
    inputs += (i, mySeed),
    outputs += (i, o)
  )

Replication(
  evaluation = myModel,
  seed = mySeed,
  sample = 100,
  aggregation = Seq(o evaluate median)
) hook display
""", name="example of replication")}

$br

The arguments for ${code{"Replication"}} are the following:

${ul(
  li{html"${code{"evaluation"}} is the task (or a composition of tasks) that uses your inputs, typically your model task and a hook."},
  li{html"${code{"seed"}} is the prototype for the seed, which will be sampled with an uniform distribution in its domain (it must ba a Val[Int] or a Val[Long]). This prototype will be provided as an input to the model."},
  li{html"${code{"sample"}} (Int) is the number of replications."},
  li{html"${code{"index"}} ($optional, Val[Int]) is an optional variable Val that can provide you with a replication index for each replication."},
  li{html"${code{"distributionSeed"}} ($optional, Long) is an optional seed to be given to he uniform distribution of the seed (\"meta-seed\"). Providing this value will fix the pseudo-random sequence generated for the prototype ${code{"seed"}}."},
  li{html"${code{"aggregation"}} ($optional) is a list of aggregations to be performed on the outputs of your evaluation task."}
)}

${h3{"Hook"}}

The ${code{"hook"}} keyword is used to save or display results generated during the execution of a workflow.
The generic way to use it is to write either ${code{"hook(workDirectory / \"path/of/a/file.csv\")"}} to save the results in a CSV file, or ${code{"hook display"}} to display the results in the standard output.

$br$br

There are some arguments specific to the ${code{"Replication"}} task which can be added to the hook:

${ul(
  li{html"${code{"output"}} is to choose what to do with the results as shown above, either a file path or the word ${code{"display"}},"},
  li{html"${code{"values = Seq(i, j)"}} specifies which variables from the data flow should be saved or displayed, by default all variables from the dataflow are used,"},
  li{html"${code("""header = "Col1, Col2, ColZ"""")} customises the header of the CSV file to be created with the string it receives as a parameter, please note that this only happens if the file doesn't exist when the hook is executed,"},
  li{html"${code{"arrayOnRow = true"}} forces the flattening of input lists such that all list variables are written to a single row/line of the CSV file, it defaults to ${code{"false"}},"},
  li{html"${code{"includeSeed"}} is a ${code{"Boolean"}} to specify whether you want the seed from the ${code{"Replication"}} task to be saved with the workflow variables, ${code{"false"}} by default."}
)}

Here is a use example:

$br$br

${hl.openmole("""
val mySeed = Val[Int]
val i = Val[Int]
val o = Val[Double]

val myModel =
  ScalaTask("import scala.util.Random; val rng = new Random(mySeed); val o = i * 2 + 0.1 * rng.nextDouble()") set (
    inputs += (i, mySeed),
    outputs += (i, o)
  )

Replication(
  evaluation = myModel,
  seed = mySeed,
  sample = 100
) hook(
  output = display,
  values = Seq(i),
  includeSeed = true)
""", name="hook replication")}

$br

For more details about hooks, check the corresponding ${a("Language", href := DocumentationPages.hook.file)} page.""")
