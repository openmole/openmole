package org.openmole.site.content.language.advanced

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

object Transition extends PageContent(html"""

In OpenMOLE, transitions represent the links between tasks.
The following examples illustrate several kinds of transitions available in OpenMOLE.


${h2{"Basic transitions"}}

${h3{"Simple transition"}}

A transition specifies a precedence relationship between two tasks.
In the following example, the task ${code{"t1"}} produces a variable ${code{"i"}} which travels along a transition to the task ${code{"t2"}}.
${code{"t2"}} uses it in turn in order to compute its output. Simple transitions are marked by the operator ${code{"--"}} between the tasks:

${hl.openmole("""
val d = Val[Double]
val e = Val[Double]

val t1 = ScalaTask("val d = 42.0") set ( outputs += d )
val t2 = ScalaTask("val e = d / 42") set ( inputs += d, outputs += e)

t1 -- (t2 hook DisplayHook())
""")}


${h3{"Exploration transition"}}

The ${b{"Exploration"}} transition links an ${code{"ExplorationTask"}} to another task.
It creates one new execution stream by sample point in the design of experiment of the ${code{"ExplorationTask"}}.
For instance, the following workflow runs the task ${code{"t1"}} 10 times.
Exploration transitions use ${code{"-<"}} to link the tasks.

${hl.openmole("""
// Declare the variable
val d = Val[Double]

// Define the Hello task
val t1 = ScalaTask("d = d * 2") set ( inputs += d, outputs += d )

//Define the exploration strategy
val exploration = ExplorationTask(d in (0.0 to 99.0 by 10.0))

exploration -< (t1 hook DisplayHook())""")}

You can read more about ${a("Explorations and Samplings", href := DocumentationPages.samplings.file)} in the relevant section of the documentation.


${h3{"Aggregating results from an exploration"}}

We have seen how we can execute tasks for a set of values with the exploration transition ${code{"-<"}}.
It is also possible to collect all the results produced by an exploration in order to compute global indicators with the aggregation transition noted ${code{">-"}}.
The following workflow sums over all the results computed by the ${code{"t1"}} task in the exploration:

${hl.openmole("""
// Declare the variable
val d = Val[Double]

val t1 = ScalaTask("d = d * 2") set ( inputs += d, outputs += d )

val exploration = ExplorationTask( d in (0.0 to 99.0 by 10.0) )

val aggregate = ScalaTask("val d = input.d.sum") set (
  inputs  += d.toArray,
  outputs += d
)

exploration -< t1 >- (aggregate hook DisplayHook())
""")}

It is very important to understand that this transition ${b{"gathers"}} input data from the dataflow.
This task has a ${b{"single instance"}} which is provided with a ${b{"collection of inputs"}} stored in variable ${code{"d"}}.
In order for OpenMOLE to match the input data to this aggregation, we explicitly note the inputs as being arrays using the ${code{"toArray"}} conversion.
Subsequent parallelism is preserved by marking the same ${code{"d"}} collection again as an array.
This restores any subsequent parallelism by splitting the data among multiple instances of the next task in the workflow.


${h2{"Combining transitions"}}

${h3{"Simple combination"}}

In order to automate some processes we might want to run several task in sequence after an exploration transition.
To achieve that, you can easily compose different transitions:

${hl.openmole("""
val d = Val[Double]

val t1 = ScalaTask("d = d * 42") set ( inputs += d, outputs += d )
val t2 = ScalaTask("d = d + 100") set ( inputs += d, outputs += d)
val exploration = ExplorationTask( d in (0.0 to 99.0 by 10.0) )

exploration -< t1 -- (t2 hook display)
""")}


${h3{"Tasks in parallel"}}

In OpenMOLE you can also declare tasks to be independent from each other so they can be executed concurrently.
Parallel tasks are to be put in brackets.
For instance, in this example ${code{"t2"}} and ${code{"t3"}} are executed concurrently:

${hl.openmole("""
val d = Val[Double]

val t1 = ScalaTask("d = d * 42") set ( inputs += d, outputs += d )
val t2 = ScalaTask("d = d + 100") set ( inputs += d, outputs += d)
val t3 = ScalaTask("d = d - 100") set ( inputs += d, outputs += d)

val exploration = ExplorationTask( d in (0.0 to 99.0 by 10.0) )

exploration -< t1 -- (t2 hook display, t3 hook display)
""")}


${h2{"Conditions"}}

When needed, it is possible to set a condition on a transition, so that the task after the transition is executed under this condition only.
Conditional transitions are specified using the ${code{"when"}} keyword.


${h3{"Using a condition on the execution of a task"}}

For instance we can add a condition on the transition towards ${code{"t2"}} in the previous workflow:

${hl.openmole("""
val d = Val[Double]

val t1 = ScalaTask("d = d * 42") set ( inputs += d, outputs += d )
val t2 = ScalaTask("d = d + 100") set ( inputs += d, outputs += d)
val exploration = ExplorationTask( d in (0.0 to 99.0 by 10.0) )

exploration -< t1 -- (t2 hook display) when "d > 1000"
""")}

In this case the task ${code{"t2"}} is executed only if the variable ${code{"d"}} is greater than 1000.


${h3{"Using a condition to define a resumable workflow"}}

Sometimes you might want to be able to easily stop your workflow and relaunch it only on unfinished jobs.
The following example explains how to relaunch your workflow only on unprocessed inputs or with new inputs.
For instance if your task may fail or sometimes return empty or invalid files, you can benefit from this OpenMOLE construct.

$br

You will need to use a combination of the ${code{"when"}} condition and the ${code{"Expression"}} keyword: the condition is expressed using an OpenMOLE ${code{"Expression"}}.
Here is an example:

${hl.openmole(s"""
// define files and name used
val parameter = Val[Int]
val resultFile = Val[File]

// write the parameter in a file
val writeTask = ScalaTask($tq
  val resultFile = newFile();
  resultFile.content = s"$$parameter"
  $tq) set (
    (inputs, outputs) += parameter,
    outputs += resultFile
  )

// outputpath is the path where resultFile is stored
val outputPath = Expression[File](workDirectory / "results/results_$${parameter}")

// the file coming from each job is copied in the results folder
val copy = CopyFileHook(resultFile, outputPath)

// a job is executed only if there is no matching resultFile or an empty resultFile
// once you've run this workflow once you can manually suppress some result files and run it again

DirectSampling(
  sampling = parameter in (0 to 100),
  evaluation = writeTask hook copy,
  condition = !outputPath.exists || outputPath.isEmpty
)
""")}



${h2{"Advanced concepts"}}

${h3{"Capsules and Slots"}}

Tasks are not directly linked to each other by transitions.
This has been made as transparent as possible, but two other notions are involved behind the scenes.
Tasks are encapsulated in so called ${code{"Capsules"}}, which can have several input ${code{"Slots"}}.
The ${a("Capsule section", href := DocumentationPages.capsule.file)} explains this in details.


${h3{html"Combining several workflow parts with ${code{"&"}}"}}

In OpenMOLE, the representation of the workflow has been designed to be as linear as possible, but actually workflows are just as graph of task and transitions.
Sometime you cannot express complex workflows in such a linear manner.
Therefore you may want to use the ${code{"&"}} operator to merge different part of a workflow.
While the linear representation is more compact, the @code{&} notation provides you with more freedom in the design of the transition graph (note that you can combine the two representations in order to get both compactness and flexibility).
The following example exposes two equivalent workflows, the first is using the linear representation and the second design using mostly the ${code{"&"}} operator:

${hl.openmole("""
val d = Val[Double]

val t1 = ScalaTask("d = d * 42") set ( inputs += d, outputs += d )
val t2 = ScalaTask("d = d + 100") set ( inputs += d, outputs += d)
val t3 = ScalaTask("d = d - 100") set ( inputs += d, outputs += d)
val exploration = ExplorationTask( d in (0.0 to 99.0 by 10.0) )

exploration -< t1 -- (t2 hook display, (t3 hook display))

(exploration -< t1) & (t1 -- t2) & (t1 -- t3) & (t2 hook display) & (t3 hook display)
""")}


${h3{"Loops"}}

Loops are a direct application of the explicit definition of ${code("Capsules")} and ${code("Slots")} to wrap tasks.
A task may possess multiple input Slots.
${code{"Slots"}} are useful to distinguish loops from synchronization points.
The execution of a task is started ${b{html"when all the incoming transitions belonging to the same input ${code{"Slot"}} have been triggered"}}.
See how several Slots define a loop in this workflow:

${hl.openmole("""
val i = Val[Int]
val t0 = ScalaTask("val i = 0") set (outputs += i)
val t1 = ScalaTask("i = i + 1") set ((inputs, outputs) += i)

(t0 -- (t1 hook display)) & (t1 -- Slot(t1) when "i < 100")
""")}


""")


