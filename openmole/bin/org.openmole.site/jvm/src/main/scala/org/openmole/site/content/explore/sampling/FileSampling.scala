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

object FileSampling extends PageContent(html"""


${h2{"Exploring a set of files"}}

Data processing often involves manipulating a large number of files, and browsing through these files.
This is why OpenMOLE features some file exploration functions to manipulate your datasets as easily as possible.


${h3{"Explore files in one directory"}}

For instance, to run a model over a set of files in the subdirectory ${code{"dir"}} of your workspace, you may use:

$br$br

${hl.openmole("""
  val f = Val[File]

  DirectSampling(
    evaluation = myModel,
    sampling = f in (workDirectory / "dir")
  )
""", header = "val myModel = EmptyTask()")}

$br

The ${code{"filter"}} modifier filters the initial file according to a predicate (here ${code{"f"}} needs to be a directory whose name begins with ${b{"exp"}}).
You can filter using any function taking a ${code{"File"}} and computing a ${code{"Boolean"}} (see the corresponding ${aa("javadoc", href := shared.link.javaFile)} or create your own).
Some predicate functions available out of the box are ${code{"startsWith()"}}, ${code{"contains()"}}, or ${code{"endsWith()"}}:

$br$br

${hl.openmole("""
  val f = Val[File]

  DirectSampling(
    evaluation = myModel,
    sampling = f in (workDirectory / "dir").files.filter(f => f.getName.endsWith(".nii.gz"))
  )
""", header = "val myModel = EmptyTask()")}


${h3{"Explore files in several subdirectories"}}

Searching in deep file trees can be very time consuming and irrelevant if you know how your data is organised.
By default, the file selector only explores the direct level under the directory you have passed as a parameter.
If you want it to explore the whole file tree, you can set the ${code{"recursive"}} option to ${code{"true"}}.

$br

To explore files located in several directories of your workspace, use:

$br$br

${hl.openmole("""
  val i = Val[Int]
  val f = Val[File]

  DirectSampling(
    evaluation =  myModel,
    sampling =
      (i in (0 to 10)) x
      (f in ListFileDomain((workDirectory / "dir"), "subdir${i}", recursive = true).filter(f => f.isDirectory && f.getName.startsWith("exp")))
  )
""", header = "val myModel = EmptyTask()")}

$br

"subdir$${i}" allows you to select one single file for each value of ${code{"i"}}.



${h2{"Files vs Paths"}}

As its name suggests, the ${code{"files"}} selector manipulates ${code{"File"}} instances and directly injects them in the dataflow.
If you plan to delegate your workflow to a ${aa("local cluster environment", href :=  DocumentationPages.cluster.file)} equipped with a shared file system across all nodes, you don't need data to be automatically copied by OpenMOLE.

$br

In this case, you might prefer the paths selector instead.
Paths works exactly like files and accept the very same options.
The only difference between the two selectors is that ${code{"paths"}} will inject ${code{"Path"}} variables in the dataflow.

$br

However, a path describes a file's location but not its content.
The explored files won't be automatically copied by OpenMOLE when using ${code{"Path"}}, so this does not fit a grid environment for instance:

$br$br

${hl.openmole("""
  import java.nio.file.Path

  val dataDir     = "/vol/vipdata/data/HCP100"

  val subjectPath = Val[Path]
  val subjectID   = Val[String]

  DirectSampling(
    evaluation = myModel,
    sampling = subjectPath in ListPathDomain(dataDir, filter = ".*\\.nii.gz") withName subjectID
  )
""", header = "val myModel = EmptyTask()")}

$br

More details on the difference between manipulating ${code{"Files"}} and ${code{"Paths"}} can be found in the dedicated entry of the ${aa("FAQ", href:= DocumentationPages.faq.file + "#WhenshallIusePathoverFile?")}.


${h2{"Going further"}}

You can find full examples using OpenMOLE's capabilities to process a dataset in the following entries of the marketplace:

${ul(
  li("FSL-Fast"),
  li("Random Forest")
  )}

Files can also be injected in the dataflow through ${aa("Sources", href := DocumentationPages.source.file)}.
They provide more powerful file filtering possibilities using regular expressions, and can also target directories only.

""")
