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

object Julia extends PageContent(html"""

${h2{"JuliaTask syntax"}}

${h3{"Preliminary remarks"}}

${Native.preliminary("JuliaTask")}

$br

The ${code{"JuliaTask"}} relies on an underlying ${code{"ContainerTask"}}.

${h3{"Arguments of the JuliaTask"}}

It takes the following arguments :

${ul(
   li{html"${code{"script"}} String or File, $mandatory. The Python script to be executed."},
   li{html"${code{"version"}} String, $optional. The version of Julia to run."},
   li{html"${code{"install"}} Sequence of strings, $optional (default = empty). The commands to be executed prior to any Julia packages installation and script execution (to install libraries on the system)."},
   li{html"${code{"libraries"}} Sequence of strings, $optional (default = empty). The name of Julia libraries that need to be installed before the script execution (note: as detailed below, installations are only achieved during the first execution of the script, and then stored in a docker image in cache. To force an update, use the ${i{"forceUpdate"}} argument)."},
   li{html"${code{"forceUpdate"}} Boolean, $optional (default = false). Should the libraries installation be forced (to ensure an update for example). If true, the task will perform the installation (and thus the update) even if the library was already installed."},
   li{html"${code{"prepare"}} Sequence of strings, $optional (default = empty). System commands to be executed just before to the execution of Julia on the execution node."}
)}

${h2{"Embedding a Julia script"}}

The toy Julia script for this test case is:

${code("""
numericaloutput = arg * 2

write(open("output.txt","w"),string("Hello world from Julia #",arg))
""")}

We save this to ${i{"hello.jl"}} and upload it in your OpenMOLE workspace.
You can then use the following script:

${hl.openmole("""
   // Declare the variable
   val arg = Val[Int]
   val arg2 = Val[Double]
   val numericaloutput = Val[Int]
   val fileoutput = Val[File]

   // julia task
   val juliaTask =
     JuliaTask(workDirectory / "hello.jl") set (
       inputs += arg.mapped,
       inputs += arg2.mapped,
       outputs += arg,
       outputs += numericaloutput.mapped,
       outputs += fileoutput mapped "output.txt"
     )

   val env = LocalEnvironment(2)

   DirectSampling(
     evaluation = juliaTask,
     sampling = (arg in (0 to 10)) x (arg2 is 2.0)
   ) hook (workDirectory / "result") on env
""")}

Notions from OpenMOLE are reused in this example.
If you're not too familiar with ${a("Environments", href := DocumentationPages.scale.file)}, ${a("Groupings", href := DocumentationPages.scale.file + "#Grouping")}, ${a("Hooks", href := DocumentationPages.hook.file)} or ${a("Samplings", href := DocumentationPages.samplings.file)}, check the relevant sections of the documentation.

""")
