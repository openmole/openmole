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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, name => _, _}
import org.openmole.site._
import org.openmole.site.tools.*
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Environment.*


object Hook extends PageContent(html"""

${h2{"Default hooks"}}

${h3{"What is a Hook?"}}

Tasks in OpenMOLE are mute pieces of software.
They are not conceived to write files, display values, or generally present any side effects at all.
The role of a task is to compute some output data from their input data.
That's what guarantees that their execution can be deported to other machines.

$br

OpenMOLE introduces a mechanism called ${b{"Hook"}} to save or display results generated during the execution of a workflow.
Hooks are conceived to perform an action on the outputs of the task they are plugged to.
Different hooks are available for different actions that need to be performed.

${h3{"How to plug a hook to a task"}}

The ${code{"hook"}} keyword is used to save or display results generated during the execution of a workflow.
There is only one mandatory argument to specify, the kind of ${code{"output"}} you want:
${ul(
    li{html"${code{"hook display"}} to display the results in the standard output, note that it is completely equivalent to writing ${code{"hook(display)"}} or ${code{"hook(output = display)"}}"},
    li{html"${code{"hook(workDirectory / \"path/to/a/file.csv\")"}} to save the results in a CSV file"}
)}

Let's consider this simple workflow:

${hl.openmole("""
  // Define the variable i
  val i = Val[Int]

  // Define a task which returns its input value multiplied by 2
  val hello = ScalaTask("i = i * 2") set (
    (inputs, outputs) += i
  )

  // Define an exploration task
  DirectSampling(
    evaluation = hello hook (workDirectory / "results/helloTask_${i}.csv"),
    sampling = i in (0 to 9)
  )
  """, name = "plug a hook")}

The ${code{"hook"}} is plugged to the end of the ${code{"hello"}} task in the ${code{"DirectSampling"}}, which means that every time ${code{"hello"}} finishes, the hook is executed.
Here it means that for each ${code{"i"}} value, the dataflow will be printed in even numbered files named ${b{"helloTask_0.csv"}}, ${b{"helloTask_2.csv"}}, etc., located in the ${b{"results"}} repository (which will be automatically created if it does not exist yet).


${h3{"Default hooks"}}

Most OpenMOLE methods come with a default hook to save their results in a properly formatted file.
To use these embedded hooks, you can directly give the required arguments (${i{"e.g."}} the path of the created file) to the ${code{"hook"}} keyword.

$br

The specific arguments of the default hooks for each method, when they exist, are described in the corresponding documentation page in the ${a("Explore", href := explore.file)} section.

${h2{"Hooks to write into files"}}
${h3{"Write a string"}}

Any string can be appended to a file using the hook ${code{"AppendToFileHook"}}.
The appended string can be a combination of variables from the data flow and plain text.

${hl.openmole("""
    val i = Val[Int]

    val h = AppendToFileHook(workDirectory / "path/of/the/file.txt", "string ${i} to append")
""", name = "append to file hook")}


${h3{"Write an entire file"}}

${code{"AppendToFileHook"}} can be used to write an entire file as well.

${hl.openmole("""
    val file = Val[File]
    val i = Val[Int]

    val h = AppendToFileHook(workDirectory / "path/to/a/file/or/dir${i}.csv", "${file.content}")
""", name = "append file to file hook")}

The path to the new file can be expanded using variables from the data flow (${code{"i"}} here for instance).
The variables or expressions written between ${b{"${}"}} are evaluated and replaced with their value.


${h3{"Write into a CSV file"}}

The hook ${code{"CSVHook"}} takes data from the data flow and appends it to a file formatted as CSV.

${hl.openmole("""
    val i = Val[Int]

    val h = CSVHook(workDirectory / "path/to/a/file/or/dir${i}.csv")
""", name = "csv hook")}

Some additional optional parameters can be passed to the ${code{"CSVHook"}}:
${ul(
   li{html"${code{"values = Seq(i, j)"}} specifies which variables from the data flow should be written in the file. The default behaviour when this list is not specified is to dump all the variables from the dataflow to the file."},
   li{html"${code{"header = \"Col1, Col2, ColZ\""}} customises the header of the CSV file to be created with the string it receives as a parameter. Please note that this only happens if the file doesn't exist when the hook is executed."},
   li{html"${code{"arrayOnRow = true"}} forces the flattening of input lists such that all list variables are written to a single row/line of the CSV file."},
)}

${hl.openmole("""
    val i = Val[Int]
    val j = Val[Array[Int]]

    val h = CSVHook(workDirectory / "path/to/a/file/or/dir${i}.csv", values = Seq(i, j), header = "i, j", arrayOnRow = true)""", name = "csv hook with options")}


${h3{"Write a matrix into a file"}}

Some workflows may output two dimensional data, which can be understood as a matrix.
For this, the ${code{"MatrixHook"}} writes matrix-like data to a file.

${hl.openmole("""
    val matrix = Val[Array[Array[Double]]]

    val h = MatrixHook("file.csv", matrix)
""")}

Output format will be a CSV file.
Data understood as matrix are one and two dimensional arrays of double, int and long.

${h2{"Hook to copy a file"}}

The ${code{"CopyFileHook"}} makes it possible to copy a file or directory from the data flow to a given location on the machine running OpenMOLE.

${hl.openmole("""
  val file = Val[File]
  val i = Val[Int]

  val h = CopyFileHook(file, workDirectory / "path/to/copy/the/file${i}.txt")
""", name = "copy file hook")}



${h2{"Hooks to display results"}}

${h3{"Display variables"}}

To display a variable ${code{"i"}} from the workflow in the standard output, use the hook ${code{"DisplayHook(i)"}}:

${hl.openmole("""
  val i = Val[Int]
  val j = Val[Int]

  val h = DisplayHook(i, j)
""", name = "to string hook")}

If no variable is specified in ${code{"DisplayHook()"}}, the whole data flow will be displayed.


${h3{"Display strings"}}

To display a string in the standard output, use the ${code{"DisplayHook(\"string\")"}}.
The string can be formed of plain text and/or variables.
You can think of the ${code{"DisplayHook"}} as an OpenMOLE equivalent to Scala's ${code{"println"}}.

${hl.openmole("""
  val i = Val[Int]

  val h = DisplayHook("The value of i is ${i}.")
""", name = "display hook")}


${h2{"Variable restriction"}}
You may want to restrict the hooked variables to a subset, like a variable filter.
You can use the following notation in the hook function {${code{"hook(yourHookHere, values = Seq(i,j))"}}}

${hl.openmole("""
  val i = Val[Int]
  val j = Val[Int]
  val k = Val[Double]

  val task = EmptyTask() set (
    outputs += (i, j, k)
  )

  task hook(display, values = Seq(i, j)) // only i and j are hooked
  task hook(workDirectory / "results/res.csv", values = Seq(j,k)) // only j and k are appended to the file
""", name = "variable restriction")}


${h2{"Conditional hooking"}}

You may want to filter outputs that are redirected to a hook, ${i{"i.e."}} do conditional hooking.
You can use for that the ${code{"when"}} keyword, built from a hook and a condition:

${hl.openmole("""
  val i = Val[Int]

  val display = DisplayHook("The value of i is ${i}.") when "i > 0"
  """, name = "condition hook")}

Decorators exist for a simpler syntax: ${code{"ConditionHook(myHook,myCondition)"}} is equivalent to ${code{"myHook when myCondition"}} and ${code{"myHook condition myCondition"}} (where the condition can be given as a condition or a string).

""")


