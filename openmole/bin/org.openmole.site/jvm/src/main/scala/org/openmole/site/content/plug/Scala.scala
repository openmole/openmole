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

object Scala extends PageContent(html"""

${h2("Simple ScalaTask")}

You can write Scala code to be executed in the workflow using the ${code("ScalaTask")}. For instance, the following workflow sums all the elements of an array using a ${code("ScalaTask")}, and displays the result. Similarly, you could use such a task to generate some model parameter values or perform some data analysis. To get more details on the hook part you can check the doc on ${a("hooks", href := DocumentationPages.hook.file)}.

${hl.openmole("""
    // Define variables
    val array = Val[Array[Double]]
    val result = Val[Double]

    // Define a task
    val sum = ScalaTask("val result = array.sum") set (
        inputs += array,
        outputs += result,

        // Default value
        array := Array(8.0, 9.0, 10.0)
    )

    // Define the workflow
    sum hook display
""")}

You can also plug you own Scala/Java code and libraries in OpenMOLE using an OpenMOLE ${a("Plugin", href := DocumentationPages.pluginDevelopment.file)}.


${h3("Useful functions")}

In addition to Scala code, OpenMOLE provides ${aa("a few useful functions", href := DocumentationPages.scalaFunction.file)} to aggregate data, create files, create random number generators, etc.


${h3("File Management")}

To learn how to provide pre-existing files to a ${code{"ScalaTask"}}, how to write into files or use files created by previous tasks in the workflow, you can go to the ${aa("file management page", href := DocumentationPages.fileManagement.file)}.



${h2("Plugins")}

In order to use code from an ${a("OpenMOLE plugin", href := DocumentationPages.pluginDevelopment.file)} in a task, you need to associate the plugin to the task, otherwise, OpenMOLE will not know how to execute your task.
To do so, use the ${code{"plugins"}} keyword and the ${code{"pluginsOf"}} function.
${code{"pluginsOf"}} takes an object from your plugin as parameter, or a class if you use ${code{"[]"}}:
${ul(
    li{html"for a class: ${code{"plugins += pluginsOf[namespace.MyClass]"}},"},
    li{html"for an object: ${code{"plugins += pluginsOf(namespace.MyObject)"}}."}
)}

 For example, this ${code{"ScalaTask"}} uses an OpenMOLE plugin containing the namespace ${code{"myOpenmolePlugin"}} that itself contains the object ${code{"Hello"}}, which is used by the ${code{"ScalaTask"}}:

$br$br

${hl.openmole("""
    // Declare the variables
    val i = Val[Int]
    val j = Val[Int]

    // Hello task
    val hello = ScalaTask("val j = myopenmoleplugin.Hello.world(i)") set (
        inputs += i,
        outputs += (i, j),

        plugins += pluginsOf(myopenmoleplugin.Hello)
    )

    // Workflow
    DirectSampling(
        evaluation = hello hook display,
        sampling = i in (0 to 2)
    )
""", header = "object myopenmoleplugin { object Hello {} }")}

""")


