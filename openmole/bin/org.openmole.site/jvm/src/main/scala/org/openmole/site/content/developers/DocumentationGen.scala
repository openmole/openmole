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

object DocumentationGen extends PageContent(html"""


${h2{"Get ready"}}

You will need the following tools to design your plugin:
${ul(
  li(html"The ${aa("git", href := shared.link.git)} software"),
  li(html"${aa("SBT", href := shared.link.sbt)}, the Scala Building Tool")
)}

The first step is to clone the github repository for OpenMOLE :

$br

${hl.code("""
git clone https://gitlab.openmole.org/openmole/openmole.git
""")}

In order to generate documentation, openMOLE need some libraries.
Into your recently cloned openmole folder, call these three commands to compile libraries:

${hl.code("""
git lfs fetch
(cd build-system && sbt publishLocal)
(cd libraries && sbt publishLocal)
""")}

${h2{"Compiling documentation"}}

You're now ready to compile the actual documentation.
Move into the nested openmole folder and run sbt to generate the website:

${hl.code("""
cd openmole
sbt buildSite
""")}

The generated site is visible by opening ${code{"openmole/openmole/bin/org.openmole.site/jvm/target/site/index.html"}} in your browser !

${h2{"Adding a new page"}}

Scala documentation file are located into the bin folder ${code{"bin/org.openmole.site/jvm/src/main/scala/org/openmole/site/content"}}:

${hl.plain("""
cd openmole/openmole/bin/org.openmole.site/jvm/src/main/scala/org/openmole/site/content
""")}

For this example, we try to add this current page "Documentation Generation" to ${a("Developers", href := DocumentationPages.developers.file)}.

 Into your favorite IDE :
 ${ul(
   li{html"open ${code{"/org/openmole/site/Pages.scala"}} and search the ${code{"def developersPages"}} entry"},
   li{html"add this new ${code{"compileDocumentation"}} variable"}
 )}

${hl.code("""
lazy val compileDocumentation = DocumentationPage.fromContent(name = "Documentation generation", content = org.openmole.site.content.developers.DocumentationGen)
""")}

After that, we add ${code{"compileDocumentation"}} to the ${code{"pageNode"}} corresponding to the "Developers" entry on the website:

${hl.code("""
def developersPages = pageNode(developers, Vector(console, pluginDevelopment, extensionAPI, restAPI, documentationGen))
""")}

Now, leave the ${code{"Pages.scala"}} file and create a new scala content file ${code{"DocumentaionGen.scala"}} into ${code{"org/openmole/site/content/community/documentation/developers/"}}.

In this file you should add some headers:
${hl.code(s"""
//TODO Fix it after Scala 3 migration
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.content.Native._

object DocumentationGen extends PageContent(html$tq

$tq)
""")}

We add a new link to the list of pages using:

${hl.code("""
@li{How to compile and modify the @a("documentation", href := DocumentationPages.documentationGen.file)}
""")}

""")
