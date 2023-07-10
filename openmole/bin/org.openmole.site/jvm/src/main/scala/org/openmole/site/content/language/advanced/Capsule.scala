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

object Capsule extends PageContent(html"""


${h2{"Definition"}}

Tasks are not directly linked to each-other by transitions.
This has been made as transparent as possible, but two other notions are involved behind the scenes.
Tasks are encapsulated in a so called ${code{"Capsule"}}.
Each ${code{"Capsule"}} has one or several input ${code{"Slots"}} which transitions are plugged to.
This code snippet explicitly encapsulates the task ${code{"t1"}} in the Capsule ${code{"c1"}}:

${hl.openmole("""
  val t1 = ScalaTask("1 + 1")
  val c1 = Capsule(t1)
""")}

Capsules are the atomic element in the workflow which transitions are plugged to.
Capsules also serve as an entry point on which ${a("Hooks", href := DocumentationPages.hook.file)}, ${a("Sources", href := DocumentationPages.source.file)} and ${a("Execution Environments", href := DocumentationPages.scale.file)} are specified.
When a task is directly linked to another without explicitly specifying a Capsule, ${b{"a single capsule is created for this task and used each time the task in mentioned in the workflow"}}.

$br

Capsules might own several input Slots in which transitions are plugged.
Slots make it possible to specify iterative workflows (with cycles) as well as synchronisation points between several parts of a workflow.
The rule is that the task encapsulated in the Capsule is executed each time all the transitions reaching a given input slot have been triggered.
To specify slots explicitly you should write:

${hl.openmole("""
  val t1 = ScalaTask("1 + 1")
  val c1 = Capsule(t1)
  val s1 = Slot(c1)
""")}

Other specific capsules are defined in OpenMOLE.
They are described in the ${a("Advanced capsule", href :=  DocumentationPages.capsule.file)} section.


${h2{"Strainer capsule"}}

In a general manner you are expected to specify the inputs and outputs of each task.
Capsules' strainer mode transmits all the variables arriving through the input transition as if they were inputs and ouptuts of the task.

$br

For instance, variable ${i{"i"}} is transmitted to the hook without adding it explicitly in input and output of the task ${i{"t2"}}, in the following workflow:

${hl.openmole("""
val i = Val[Int]
val j = Val[Int]

val t1 = ScalaTask("val i = 42") set (outputs += i)
val t2 = ScalaTask("val j = 84") set (outputs += j)

t1 -- (Strain(t2) hook DisplayHook(i, j))
""")}

This workflow displays ${code{"{i=42, j=84}"}}

${h2{"Master capsule"}}

OpenMOLE provides a very flexible workflow formalism.
It even makes it possible to design workflows with a part that mimics a ${b{"master / slave"}} distribution scheme.
This scheme involves many slave jobs computing partial results and a master gathering the whole result.

$br

You can think of a steady state genetic algorithm, for instance, as a typical use case.
This use case would see a global solution population maintained and a bunch of slave workers computing fitnesses in a distributed manner.
Each time a worker ends, its result is used to update the global population and a new worker is launched.
To achieve such a distribution scheme, one should use the ${i{"Master Capsule"}} along with an end-exploration transition.

$br

The ${code{"MasterCapsule"}} is a special capsule that preserves a state from one execution to another.
An execution of the ${code{"MasterCapsule"}} modifies this state and the next execution gets the state that has been modified last.
To ensure soundness of the state only, the ${code{"MasterCapsule"}}s are always executed locally and multiple executions of a given ${code{"MasterCapsule"}} are carried sequentially.

$br

By using the ${code{"MasterCapsule"}}, a workflow can evolve a global archive, and compute new inputs to be evaluated from this archive.
Even if it is not required, a ${code{"MasterCapsule"}} is generally executed in an exploration, in order to have several workers computing concurrently.
This distribution scheme suggests that all the workers should be killed when the global archive has reached a suitable state.
This is the aim of the end-exploration transition, which is noted ${code{">|"}}.

$br

The following script orchestrates a master slave distribution scheme for a dummy problem.
OpenMOLE launches 10 workers.
Along these workers, the ${code{"MasterCapsule"}} hosts the selection task.
The selection task stores the numbers that are multiple of 3 and relaunches a worker for the next value of ${code{"i"}}.
The second argument of the ${code{"MasterCapsule"}} constructor is the data that should persist from one execution of the ${code{"MasterCapsule"}} to another.

${hl.openmole("""
val i = Val[Int]
val archive = Val[Array[Int]]

val exploration = ExplorationTask(i in (0 until 10))

val model = ScalaTask("i = i + 7") set (inputs += i, outputs += i)

val select =
  ScalaTask("archive = archive ++ (if(i % 3 == 0) Seq(i) else Seq())") set (
    (inputs, outputs) += (i, archive),
    archive := Array[Int]()
  )

val finalTask = EmptyTask()

val displayHook = DisplayHook()

val skel = exploration -< model -- (Master(select, archive) hook displayHook)
val loop = select -- Slot(model)
val terminate = select >| (Strain(finalTask) hook DisplayHook("${archive.size}")) when "archive.size > 10"

skel & loop & terminate
""")}


""")


