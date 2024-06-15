package org.openmole.site.content.plug

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

object Plug extends PageContent(html"""
${h2{"Tasks"}}

Tasks are the atomic computing elements of OpenMOLE: they describe what OpenMOLE should execute.
There are a number of tasks in OpenMOLE especially designed to plug your own models and/or programs, depending on the language your model uses (binary executable, Java, R, etc.).
You need to choose the adequate task for your model.

$br

All the available tasks are documented in this section, just click on an item in the left menu list.

$br$br

The execution of a given task ${b{"depends on input variables"}}, and each task ${b{"produces output variables"}} which can be transmitted as inputs of subsequent tasks.
Below is a dummy task to illustrate this concept:

$br$br

${hl.openmole("""
    // Define two variables i and j of type Int
    val i = Val[Int]
    val j = Val[Int]

    // Instantiate a simple task using the i variable as an input, and j as an output
    // This task adds 2 to the input
    val myTask = ScalaTask("val j = i + 2") set (
        inputs += i,
        outputs += j,

        // Default value for inputs
        i := 3
    )
""")}

$br

${code{"j"}} contains the result of ${code{"myTask"}}, i.e. 5.
Any task immediately following ${code{"myTask"}} in the workflow (i.e. linked to it with a transition) will be able to use ${code{"j"}} as an input.

$br

It is also possible to specify default values for inputs, which are used by the task in case no input data was provided in the dataflow.

$br$br

Once your model is properly plugged in an OpenMOLE task, you can use an exploration method on it, and delegate the multiple executions of the task on remote computing environments.


${h2{"Exploration method"}}

The composition of a full exploration experiment is achieved by writing a script in the OpenMOLE ${a("language", href := DocumentationPages.language.file)}.
A working OpenMOLE exploration script needs to define:
${ul(
   li{html"one or several ${b{"tasks"}},"},
   li{html"their ${b{"inputs"}} and ${b{"outputs"}},"},
   li{html"an ${a("exploration method", href := DocumentationPages.explore.file)},"},
   li{html"one or several ${a("hooks", href := DocumentationPages.hook.file)},"},
   li{html"possibly an ${a("execution environment", href := DocumentationPages.scale.file)}."}
)}

Let's take the previous task, that adds 2 to an input.
You want to execute this task for a great number of different inputs and gather all the results in a single file.
The way to do it is shown in the following example:

$br$br

${hl.openmole("""
    // Define two variables i and j of type Int
    val i = Val[Int]
    val j = Val[Int]

    // Define a task that adds 2 to the input
    val myTask = ScalaTask("val j = i + 2") set (
        inputs += i,
        outputs += j,

        // Default value for inputs
        i := 3
    )

    // Define an exploration task: a direct sampling varying i
    val myExploration = DirectSampling(
        evaluation = myTask,
        sampling = (i in (0 to 1000 by 5))
    )

    // Execute the workflow
    myExploration hook (workDirectory / "results")
""")}

$br

In this script, ${code{"myTask"}} will be executed for each value of ${code{"i"}} from 0 to 1,000 with a step of 5.
Each result ${code("j")} is written in the ${i("results.omr")} file through a ${aa("hook", href := DocumentationPages.hook.file)}.

$br$br

You can find some working examples of simple exploration tasks in the ${aa("Market Place", href := DocumentationPages.market.file)}.
Be sure to also check out our ${aa("Getting Started", href := DocumentationPages.stepByStepIntro.file)} tutorial!
""")


