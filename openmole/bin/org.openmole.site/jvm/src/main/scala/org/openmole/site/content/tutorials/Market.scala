package org.openmole.site.content.tutorials

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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.content.Native._

object Market extends PageContent(html"""

${h2{"What is the Market Place for?"}}

The OpenMOLE Market Place is accessible through this ${a("repository", href := shared.link.repo.market)} or directly from the GUI application: ${i{"New Project > From Example"}}.
Clicking on a project will download it in the application, and you will be able to run it immediately!

$br

There you will find examples handling some use cases of OpenMOLE.
Don't focus too much on the applications or programming languages serving as a use case, these examples are more about how to do things the OpenMOLE way.



${h2{"Sneak peek into the Market Place contents"}}

${h3{"Hello World"}}
These examples demonstrate how to plug models written in different languages in OpenMOLE.
There are examples for R, Scilab, and Java.
For Java, there are two examples: one to plug the Java model as any other language, and the second to embed it as a plugin.


${h3{"Applicative tutorials"}}
${ul(
  li(html"${b{"Native application"}} how to package your own application on Linux to make it portable and explore it with OpenMOLE"),
  li(html"${b{"Workflow example"}} basic principles of OpenMOLE"),
  li(html"${b{"Model exploration"}}"),
  li(html"${b("Approximate Bayesian Computation")}"),
)}

Some of these tutorials are also detailed in the ${a("Tutorials", href := tutorials.file)} section.


${h3{"NetLogo examples"}}
There are a number of NetLogo examples, to illustrate various OpenMOLE methods.
Two classic NetLogo models are used in these examples: the Fire model and the Ants model.

$br

Not all examples are listed here, for a full list look at the GitHub ${aa("repository", href := shared.link.repo.market)} or in the GUI.

${img(src := Resource.img.guiGuide.market.file, width := "95%")}



${h2{"Contribute your example/tutorial!"}}

You can contribute to the market place either by making a pull request on the ${a("central OpenMOLE market place", href := shared.link.repo.market)}, or by pushing your example workflow on your own git repository and post it to the @aa("user mailing list", href:=shared.link.forum).

$br

To contribute to the market place you should only provide ${b{"self-contained working examples"}}.
All resources needed to execute the workflow should be located in a single directory.
One or several ${i(".oms")} files can be provided, and a ${i{"README.md"}} must explain what the workflow computes.
A workflow is included in this section ${b{"only if it compiles"}} within the matching OpenMOLE version.

""")
