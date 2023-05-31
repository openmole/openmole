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

import org.openmole.site.content.header.*
import Native.*

object Tutorials extends PageContent(html"""


${h2{"Tutorials"}}

In this section you will find some tutorials to help you build your first experiments.
Feel free to suggest topics for new tutorials you would find helpful on the ${aa("forum", href := shared.link.forum)}.
You can even write your own tutorial and share it with the OpenMOLE community on the forum!
We'll make sure to integrate it with the next release.

${h3{"Getting Started"}}
${ul(
    li(a("A Step by Step Introduction to OpenMOLE", href := DocumentationPages.stepByStepIntro.file)),
    li(a("How to Execute an Exploration Task", href := DocumentationPages.exploreTuto.file))
)}

${h3{"NetLogo Tutorials"}}
${ul(
    li(a("Simple Sensitivity Analysis of the Fire NetLogo Model", href := DocumentationPages.simpleSAFire.file)),
    li(a("Using Genetic Algorithms to Calibrate a NetLogo Model", href := DocumentationPages.netLogoGA.file))
)}


${h2{"Market Place"}}

In the ${a("Market Place", href := DocumentationPages.market.file)} you will find several examples of OpenMOLE use cases, to demonstrate how to apply the various exploration methods.
They are self-contained working examples, with different models written in different languages.

""")
