package org.openmole.site.content.language

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

object FileManagement extends PageContent(html"""

OpenMOLE implements mechanisms to conveniently make use of data stored as files in your experiments.

$br

${i{"Nota Bene:"}} OpenMOLE makes very few distinctions between files and directories.
In this part, most mentions of a "file" are also valid for a "directory".

$br

The files in OpenMOLE tasks have two main use cases:
${ul(
  li{html"they can be provided to a task as ${a("input files or resources", href := DocumentationPages.fileManagement.file + "#Inputfilesandresources")},"},
  li{html"they can be produced by a task ${a("output files", href := DocumentationPages.fileManagement.file + "#Outputfiles")}."}
)}

${img(src := Resource.img.model.fileMapping.file, width := "100%")}

${h2{"Input files and resources"}}

A task can be provided with input files and resources prior to its execution, in order to use theses files during execution.
The difference between input files and resources is that ${b{"input files are generally produced dynamically by another task"}} in the workflow, whereas ${b{"resources are preexisting files"}}.

$br

In OpenMOLE, there are two behaviors when it comes to manipulating input files and resources: one for the ScalaTask, and one for the other "external" tasks that are mainly used to embed code from other languages.


${h3{"External Tasks"}}
In order to provide files to that kind of tasks (Python, R, NetLogo, ...), OpenMOLE needs to copy the file to a known path on the local hard drive and feed it to the task during execution.

${h6{"Resources"}}
To provide a file as a resource to a task, it first needs to be uploaded to your working directory in the GUI (see ${a("here", href := DocumentationPages.gui.file + "#FileManagement")} for more info on how to do this).
It can then be fed to the task through the @code{resources} keyword inside the task: ${code{"resources += path/to/the/file"}}.

$br

Let's first consider a simple case in which an external task requires a file named ${code{"file.txt"}} to be executed:

${hl.openmole("""
    // Define the task
    val task = SystemExecTask("cat file.txt") set (
       resources += (workDirectory / "file.txt")
    )

    // Define the workflow
    task
""")}

We can do the same with a directory present in your OpenMOLE working directory:

${hl.openmole("""
    // Define the task
    val task = SystemExecTask("ls mydirectory") set (
      resources += workDirectory / "mydirectory"
    )

    // Define the workflow
    task
""")}

It is also possible to provide a second argument to rename the file: ${code{"resources += (path/to/the/file, \"new_name.txt\")"}}.
In this case the directory in which the task is executed contains a file with the same content but a different name.
For instance:

${hl.openmole("""
    // Define the task
   val task = SystemExecTask("cat bettername.txt") set (
     resources += (workDirectory / "file.txt", "bettername.txt")
   )

    // Define the workflow
   task
""")}

The ${code{"resources"}} keyword is useful for files existing before the workflow execution.
In other cases, you might want to produce a file in a task and provide it to a subsequent task.

${h6{"Input files"}}
Input files are files that the workflow will interact with dynamically, mainly by creating them, or writing in them.
The ${code{"inputFiles"}} keyword is used for these files, assigning a file variable from the workflow to a name and linking it to the file object created: ${code{"inputFiles += (fileVariable, \"filename.txt\""}}.

${hl.openmole(s"""
    // Define a File variable
    val f = Val[File]

    // Task to create the file and write something in it
    val producer = ScalaTask($tq
        val f = newFile()
        f.content = "I love tofu"
    $tq) set (
        outputs += f
    )

    // Task to take file f, name it "love.txt", and display it
    val task = SystemExecTask("cat love.txt") set (
        inputFiles += (f, "love.txt")
    )

    // Define the workflow: chain the two tasks
    producer -- task
""")}

${h3{"Scala Tasks"}}
Since the ${code{"ScalaTask"}} is able to directly access the file variables ${code{"Val[File]"}}, it is easier to provide it with files.
Files are just plain simple inputs, in the same manner as any other numerical inputs for instance.

${hl.openmole(s"""
    // Define a File variable
    val f = Val[File]

    // Define a task
    val task = ScalaTask($tq
        println(f.content)
    $tq) set (
        inputs += f,

        // Default value
        f := workDirectory / "file.txt"
    )

    // Define the workflow
    task
""")}

In the example above, the file is provided using a default argument.
You can of course produce it in another task.

${hl.openmole(s"""
    // Define a File variable
    val f = Val[File]

    // Define a task
    val producer = ScalaTask($tq
        val f = newFile()
        f.content = "I love tofu"
    $tq) set (
        outputs += f
    )

    // Define another task
    val task = ScalaTask($tq
        println(f.content)
    $tq) set (
        inputs += f
    )

    // Define the workflow
    producer -- task
""")}

Note that the type of variable ${code{"f"}} is ${code{"java.io.File"}}.
It can be provided as an argument to Java or Scala function calls as it is.

${h2{"Output files"}}

When files are produced by a task, they should be set as an output of this task in order to be collected and persist after the execution of the task.
These files are assigned to a variable of type ${code{"Val[File]"}} and can be transmitted to another task, or copied in the work directory using ${code{"CopyFileHook"}} (see ${a("here", href := DocumentationPages.hook.file)} for more info on hooks).
For external tasks (all but the ${code{"ScalaTask"}}, the way to collect files after a task execution is by using the ${code{"outputFiles"}} keyword.
The case of the ${code{"ScalaTask"}} is explained in a separate section.


${h3{"External Tasks"}}
The general mechanism to save a file for subsequent execution or in the work directory is to link the path of the produced file to a variable of type ${code{"Val[File]"}} using the ${code{"outputFiles"}} keyword.
${code{"outputFiles"}} gets a file with a given name and assign it to an OpenMOLE variable once the execution of the task is completed: ${code{"outputFiles += \"filename.txt\", fileVariable)"}}.

${hl.openmole("""
    // Define a file variable
    val f = Val[File]

    // Task to produce the file and save it in the f variable
    val task = SystemExecTask("echo I love tofu > output.txt") set (
        outputFiles += ("output.txt", f)
    )

    // Define the workflow: hook the task to write f in the working directory
    task hook CopyFileHook(f, workDirectory / "taskOutput.txt")
""")}


${h3{"Scala Task"}}
When using the ${code{"ScalaTask"}}, files are simple variables and are manipulated like any variable.
To output a file variable you can just set it as any usual output:

${hl.openmole(s"""
    // Define a file variable
    val f = Val[File]

    // Define a task
    val producer = ScalaTask($tq
        val f = newFile()
        f.content = "I love tofu"
    $tq) set (
        outputs += f
    )

    // Define the workflow
    producer hook CopyFileHook(f, workDirectory / "taskOutput.txt")
""")}



${h2{"Complementary information"}}

${ul(
  li{a("Documentation on data processing", href := DocumentationPages.fileSampling.file)},
  li{a("Utility functions of the ScalaTask", href := DocumentationPages.scalaFunction.file)},
  li{a("Documentation of the RTask", href := DocumentationPages.r.file)},
  li{a("Documentation of the CARETask and the SystemExecTask", href := DocumentationPages.container.file)},
  li{a("Example in Python", href := DocumentationPages.python.file)},
  li{a("Example in R", href := DocumentationPages.r.file)}
)}

""")


