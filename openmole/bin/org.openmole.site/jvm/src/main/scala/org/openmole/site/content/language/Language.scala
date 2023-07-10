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


object Language extends PageContent(html"""

The OpenMOLE language is an extension of the Scala language designed for model exploration and distributed computing.
It supports all the Scala constructs, and additional operators and classes specifically designed to compose workflows.
OpenMOLE workflows expose explicit parallel aspect of the workload that can be delegated to distributed computing environments in a transparent manner.
The philosophy of OpenMOLE is test small (on your computer) and scale for free (on remote distributed computing environments).

$br

A good way to get a first glimpse of what OpenMOLE can do is to read this ${a("research paper", href := Resource.paper.fgcs2013.file)}.

${h2{"OpenMOLE scripts"}}

The OpenMOLE scripts are stored in the GUI in ${b{".oms"}} files.
One workflow can be split into several files.

$br

To be able to use the content of a ${b{".oms"}} file (let's call it ${code{"file2.oms"}}) in another one (say ${code{"file1.oms"}}), located in the same directory, ${code{"file2.oms"}} needs to be imported in ${code{"file1.oms"}}.
The following line needs to be written at the beginning of ${code{"file1.oms"}}:

${hl.openmoleNoTest("""
    import _file_.file2._
""")}

To refer to a file located in a parent directory, use the ${code{"parent"}} keyword:

${hl.openmoleNoTest("""
    import _parent_._file_.file2._
""")}



${h2{"Basic Scala constructs"}}

You need only a very basic understanding of the Scala language in order to be able to design OpenMOLE workflows.

${h3{"Declare variables"}}

${hl.openmoleNoTest("""
    val a = 1 // declares a variable a of type Int
    val b = "Text" // declares a variable a of type String
    val c = if(condition) 5 else 10 // declare a variable c of type Int, the value of c depends on the condition
""")}


${h3{"Construct objects"}}

OpenMOLE takes advantage of the object oriented aspect of Scala.
A set of objects are available to build and assemble together to specify your workflow.
Usually, an object is instantiated using the ${code{"new"}} keyword: ${hl.openmole("""val f = new File("/tmp/file.txt")""")}

$br

However, in OpenMOLE we have chosen to use factories instead of directly constructing objects, that's why most of OpenMOLE scripts don't contain the ${code{"new"}} keyword at all.
For instance: ${hl.openmole("""val l = File("/tmp/file.txt")""")}.
Under the hood, it calls a method that is in charge of building the file.


${h3{"Named parameters"}}

Function calls generally require the parameters to be provided in a predefined order.
In Scala you can get rid of this ordering constraint by using named parameters.
OpenMOLE scripts will often make use of this pattern: ${code{"val t = SomeClass(value1, value2, otherParam = otherValue)"}}.
Here it means that ${code{"value1"}} and ${code{"value2"}} are the values for the first two (unnamed) parameters, and that the parameter named ${code{"otherParam"}} is set to the value ${code{"otherValue"}}.
Unspecified parameters are set to their default value.


${h2{"Going further"}}

What you have read so far should be sufficient in order to get started with OpenMOLE.
To begin with the OpenMOLE syntax you should have a look at the ${a("Getting started", href := DocumentationPages.stepByStepIntro.file)}.
You may also want to look at the ${a("Task documentation", href := DocumentationPages.plug.file)}, and more generally the ${a("Documentation", href := DocumentationPages.documentation.file)}.

$br

Scala is a very nice language, with an extensive and very well designed standard library.
To get more insights on this language, check these links:
${ul(
 li{aa("Scala website", href := shared.link.scala) },
 li{aa("Scala books", href := shared.link.scalaBook) },
 li{aa("Standard API documentation", href := shared.link.scalaDoc) }
)}

""")


