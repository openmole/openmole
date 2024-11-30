package org.openmole.site.content.developers

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

object ExtensionAPI extends PageContent(html"""

While OpenMOLE's core code is not intended to be directly accessible to most users, an easy-to-use transparent and flexible API has been developed for simple development of user extensions to the core.
This API allows the user to implement new tasks, methods, and samplings.

${h2{"Concepts of the API"}}

The primitives for the API are imported through the contents of the package ${code{"org.openmole.core.dsl.extension"}}.
These primitive provide constructors for:
${ul(
  li{"tasks"},
  li{"samplings"},
  li{"hooks"}
)}

${h2{"Task extensions"}}

To define a new task, use ${code{"Task(name: String)(process: FromContext => Context)"}}.
What the task does is defined by the provided closure, which transforms a ${code{"FromContext"}} into a ${code{"Context"}}.
You can add implicits in your ${code{"apply"}} method to get advanced services (mole services, network services, etc.).

$br

Validation is provided with the ${code{"validate"}} method which transforms validation parameters into a sequence of throwables.
For example:

${hl.code("""
  object MyTask {
    def apply(taskparam: Double,taskval: Val[Double])(implicit moleService: MoleServices,workspace: Workspace, networkService: NetworkService) =
      Task("MyTask"){
        parameters =>
          // do something with from context parameters : here add a constant to a double prototype
          Context(taskval -> parameters.context(taskval) + taskparam)
      } validate {vp => vp.map{proto => if(proto.v < 0) new Throwable("double proto should be positive") }} set (
        (inputs,outputs) += (taskval)
      )
  }
""")}



${h2{"Sampling extensions"}}

To implement a sampling, the constructor ${code{"Sampling"}} takes a function transforming a ${code{"FromContext"}} into a sampling result, which is an ${code{"Iterator[Iterable[Variable[?]]]"}}.
For example, the following sampling assigns uniformally a sequence of doubles to some prototypes :

${hl.code("""
  object MySampling {
    def apply(values: FromContext[Array[[Double]]],prototypes: Val[?]*) = Sampling {
     p =>
      values.from(p.context).map{ value => prototypes.toList.map{ proto => Variable(proto,value)}}.toIterator
    } validate { _ => Seq.empty} inputs {prototypes} prototypes {prototypes}
  }
""")}

${h2{"Integrating your extension into the build"}}

OpenMOLE uses OSGI bundles to integrate its different components into a single application.
This allows to load bundles dynamically for instance, which is done when adding user plugins through the GUI.
To integrate your module into the build process of OpenMOLE, several steps have thus to be performed:
${ul(
    li{html"""
    Add your module as an OSGI project into the main build file ${b{"build.sbt"}}. For example, in the case of a new sampling, the syntax would be similar to the Sobol sampling: 
    ${hl.code("""
    lazy val quasirandomSampling = OsgiProject(pluginDir, "org.openmole.plugin.sampling.quasirandom", imports = Seq("*")) dependsOn(exception, workflow, workspace, openmoleDSL) settings (
      pluginSettings,
      libraryDependencies += Libraries.math)
    """)}
    Note that you need to specify the bundles you use in your code, and the external libraries dependencies.
    You also need to add your module to the integrated ones, by adding the project to ${code{"allSamplings"}} for example.
    If your library dependency requires specific resolvers, these can be added to ${code{"defaultSettings"}}, or only locally to your project.
    """},
    li{html"""
    Add your library dependencies to the ${code{"Libraries"}} object in the file ${b{"openmole/project/Libraries.scala"}}. 
    For the code above, you would have to specify the ${code{"Libraries.math"}} value, which in this particular case is the apache common math library: ${code{"""lazy val math = "org.openmole.library" %% "org-apache-commons-math" % "3.6.1""""}}. 
    The library is defined as a bundle under the organisation org.openmole.library. The construction of this bundle is detailed in the next step."""},
    li{html"""
    Transform the required library dependencies into OSGI bundles. This step is done during the library publishing step of OpenMOLE compilation: you have thus to modify ${code{"libraries/build.sbt"}} by adding a new OSGI project, for example 
    ${hl.code("""
    lazy val math = OsgiProject(dir, "org.apache.commons.math", exports = Seq("org.apache.commons.math3.*"), privatePackages = Seq("assets.*")) settings (
      settings
      libraryDependencies += "org.apache.commons" % "commons-math3" % mathVersion, 
      version := mathVersion)""")}
    At this stage, the exports statement is important since it is thanks to it that the classes will be visible to other OSGI bundles and more particularly your plugin.
    """},
    li{html"""
    A last step may be necessary in case your dependencies also have dependencies which are already OSGI bundles.
    In that case, a conflict will occur and they must be added to a bundle filter used during the final assembly of OpenMOLE.
    This filter is defined as ${code{"bundleFilter"}} in main build. Add the names of the offending libraries in the ${code{"bundleFilter.exclude"}} function.
    """}
)}
""")
