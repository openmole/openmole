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

object Scilab extends PageContent(html"""

${h2{"ScilabTask"}}

Scilab is an open source software initially designed for numerical computation (see ${aa("the official website", href:= "http://www.scilab.org/")}).
It provides its own high-level scripting language and can be used as a computing kernel only.
Note that it includes most of proprietary Matlab's functionalities and that a set of conversion tools exists (${aa("doc", href:= "https://help.scilab.org/docs/6.0.1/fr_FR/mfile2sci.html")}).


${h3{"Preliminary remarks"}}

${Native.preliminary("ScilabTask")}


${h3{"Arguments of the ScilabTask"}}

It takes the following arguments :

${ul(
   li{html"${code{"script"}} String or file, $mandatory. The Scilab script to be executed."},
   li{html"${code{"install"}} Sequence of strings, $optional (default = empty). The commands to be executed prior to the script execution (to install libraries on the system)."},
   li{html"${code{"version"}} String, $optional. The version of Scilab to run."},
   li{html"${code{"prepare"}} Sequence of strings, $optional (default = empty). System commands to be executed just before to the execution of Scilab on the execution node."}
)}

${h3{"Simple ScilabTask"}}

Here is a dummy workflow multiplying a vector ${code{"dArray"}} by ${code{"i"}} using a ${code{"ScilabTask"}}:

$br$br

${hl.openmole("""
    // Declare variables
    val i = Val[Int]
    val dArray = Val[Array[Double]]

    // Task
    val m = ScilabTask("dArray = dArray * i") set (
        inputs += i mapped "i",
        inputs += dArray mapped "dArray",
        outputs += i,
        outputs += dArray mapped "dArray",

        // Default values
        i := 9,
        dArray := Array(9.0, 8.0)
    )

    // Workflow
    m hook DisplayHook()
""")}

${h3{"A ScilabTask running a script"}}

Here is a dummy workflow running the "rand.sce" script:

$br$br

${hl.openmole("""
val size = Val[Int]
val a = Val[Array[Array[Double]]]

val model =
  ScilabTask(workDirectory / "rand.sce") set (
    inputs += (size.mapped),
    outputs += (a.mapped("A"), size)
  )

DirectSampling(
  evaluation = model,
  sampling = size in (0 to 10 by 2)
) hook (workDirectory / "results2")
""")}

""")


