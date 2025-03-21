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


object Source extends PageContent(html"""

Sources have been designed as a possible way to inject data in the dataflow from diverse sources: CSV files, databases, sensors...

$br

At the moment, only file-based and http(s) url Sources are available in OpenMOLE.
If you need to interface OpenMOLE with an external datasource, check the ${a("contact information page", href := DocumentationPages.whoWeAre.file)} to see how to reach the OpenMOLE development team.



${h2{"Plug a source"}}

Sources are plugged in the dataflow in a similar fashion to ${a("hooks", href := DocumentationPages.hook.file )}.
Let's consider this simple worbflow:

${hl.openmole("""
val files = Val[Array[File]]
val result = Val[Double]

val hello =
  ScalaTask("val result = computeFromFiles(files)") set (
    inputs += files,
    outputs += result
  )

val s = ListFileSource(workDirectory / "directory", files)

(hello source s)
""", name = "plug a source")}

The source ${code{"s"}} is plugged at the beginning of the task ${code{"hello"}}.
The source is executed prior to each execution of ${code{"hello"}}.
You can also plug multiple sources on the same task using the syntax: ${code{"hello source (s1, s2, s3)"}}.



${h2{"List files in a directory"}}

This source lists directories and injects an array of ${code{"File"}} objects into the dataflow.
See how the range of files selected can be filtered using a regular expression as a last parameter to the source builder.

${hl.openmole("""
  val someVariable = Val[String]
  val txtFiles = Val[Array[File]]
  val files = Val[Array[File]]

  val s1 = ListFileSource(workDirectory / "directory", files)

  val s2 =
    ListFileSource(workDirectory / "/${someVariable}/", txtFiles, ".*\\.txt") set (
      inputs += someVariable
  )
""", name = "list file source")}


${h2{"List directories in a directory"}}

Likewise, you can inject an array of directories in the dataflow.
Directories are also represented as ${code{"File"}} objects.
Again, the selection can be done either by passing a complete directory name, or a global pattern that will be matched against the names of the directories found.

${hl.openmole("""
val someVariable = Val[String]
val dirs = Val[Array[File]]
val aaaDirs = Val[Array[File]]

// will fill dirs with all the subdirectories of "directory"
val s1 = ListDirectoriesSource(workDirectory / "directory", dirs)

val s2 =
  // will fill aaaDirs with all the subdirectories of "directory" starting with aaa
  ListDirectoriesSource(workDirectory / "${someVariable}", aaaDirs, "^aaa.*") set (
    inputs += someVariable
  )
""", name = "list directories source")}

Sources store each entry found in an Array.
In most cases, you will want each of the entries to feed a different task.
Let's now see how this can be done by reusing what we've discovered with the ${a("data processing sampling", href := DocumentationPages.fileSampling.file)}.


${h2{"A complete example"}}

Here, we are collecting all the directories named ${i{"care_archive"}}.
See how they are gathered in an @i{Array[File]} container and can be explored by an ${a("ExplorationTask", href := DocumentationPages.fileSampling.file)} using the keyword ${code{"in"}}.
This exploration generates one @code{analysisTask} per directory collected by the source.

${hl.openmole("""
val directoriesToAnalyze  = Val[Array[File]]

val s = ListDirectoriesSource(workDirectory / "data/care_DoE", directoriesToAnalyze, "care_archive")

val inDir = Val[File]
val myWorkDirectory = "care_archive"

val analysisTask =
SystemExecTask(s"${myWorkDirectory}/re-execute.sh") set (
  inputFiles    += (inDir, myWorkDirectory)
)

val exploration = ExplorationTask(inDir in directoriesToAnalyze)

(exploration source s) -< analysisTask""", name = "complete example with source")}


${h2{"Retrieve data from a HTTP URL"}}

You can inject data into your workflow from a HTTP/HTTPS URL using the ${code{"HttpURLSource"}}.
This is a preferred method to making network calls from within your model: when model runs are distributed on a cluster or the grid, such requests may fail.
This source will retrieve raw content from the provided URL, and attribute to a String or a File prototype (in the second case, the content is written as file content).
This is illustrated in the following example with a String prototype:

${hl.openmole("""
val stringOutput = Val[String]
val s = HttpURLSource("http://api.ipify.org",stringOutput)
ScalaTask("println(input.stringOutput)") set ((inputs,outputs) += (stringOutput)) source s hook display
""", name = "example http url source")}

and in the following example with a File prototype.

${hl.openmole("""
val fileOutput = Val[File]
val s = HttpURLSource("http://api.ipify.org",fileOutput)
ScalaTask("println(input.fileOutput)") set ((inputs,outputs) += (fileOutput)) source s hook CopyFileHook(fileOutput,"test.txt")
""", name = "example http url source file")}

""")


