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

object PluginDevelopment extends PageContent(html"""

OpenMOLE is a pluggable platform.
It means that you can easily write your own extension and plug it into OpenMOLE.
This tutorial explains how to write an OpenMOLE plugin using Scala and SBT.
OpenMOLE is based on the JVM so you can create OpenMOLE plugins using Scala or any other JVM based languages such as Java, Groovy, Clojure, Jython, etc.

${h2{"Get ready"}}

You will need the following tools to design your plugin:
${ul(
  li(html"The ${aa("git", href := shared.link.git)} software."),
  li{html"${aa("SBT", href := shared.link.sbt)}, the Scala Building Tool."}
)}

${hl.code("""
git clone https://github.com/openmole/myopenmoleplugin.git
""")}

This repository contains a template to help you create OpenMOLE plugins easily.
The ${i{"hello"}} directory contains the source code of the plugin and the materials to build it:

${hl.code("""
package myopenmoleplugin

object Hello {
  def world(i: Int) = i * 2
}
""")}



${h2{"Build your plugin"}}

The file @b{build.sbt} contains the building instructions for SBT.
The most important part are the OSGi instructions:

${hl.plain(s"""
enablePlugins(SbtOsgi)

OsgiKeys.exportPackage := Seq("myopenmoleplugin.*")

OsgiKeys.importPackage := Seq("*;resolution:=optional")

OsgiKeys.privatePackage := Seq("*")

OsgiKeys.requireCapability := ${tq}osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"${tq}
""")}

${ul(
  li(html"${code{"exportPackage"}} instruction makes the @code{myopenmoleplugin} package visible to OpenMOLE."),
  li(html"${code{"importPackage"}} instruction means that every package that is not included into the plugin should be imported."),
  li(html"${code{"privatePackage"}} means that every package in the project, or in the dependencies, should be embedded except for the package starting by the \"scala\" word. The scala packages provided by OpenMOLE will be used by the plugin instead.")
)}

To build the plugin, execute @code{sbt osgiBundle}.
SBT will then construct the plugin in ${code{"target/scala-3.x.x/myopenmoleplugin_3.x.x-1.0.jar"}}.
This JAR file contains the classes you have developed (*.class) along with the metadata relative to imports and exports in the ${code{"MANIFEST.INF"}} file:

${hl.plain("""
META-INF/MANIFEST.MF
myopenmoleplugin/
myopenmoleplugin/Hello$.class
myopenmoleplugin/Hello.class
""")}

You can check in the ${i{"MANIFEST.MF"}} that your namespace is exported.



${h2{"Import your plugin"}}

To enable your plugin in OpenMOLE, either use the plugin panel in the GUI, or use the option -p:

${hl.plain("""
openmole -p target/scala-2.12/myopenmoleplugin_3.x.x-1.0.jar
""")}

You can now use the ${code{"Hello"}} object in your workflows:

${hl.openmole("""
// Declare the variable
val i = Val[Int]
val j = Val[Int]

// Hello task
val hello = ScalaTask("val j = myopenmoleplugin.Hello.world(i)") set (
  inputs += i,
  outputs += (i, j),
  plugins += pluginsOf(myopenmoleplugin.Hello)
)

DirectSampling(
  evaluation = hello,
  sampling = i in (0 to 2)
) hook display
""", header = "object myopenmoleplugin { object Hello {} }")}

""")
