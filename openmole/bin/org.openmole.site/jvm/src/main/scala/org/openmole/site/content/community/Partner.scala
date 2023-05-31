package org.openmole.site.content.community

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

object Partner extends PageContent(html"""
${
  div(flexRow) {
    a(img(src := Resource.img.partner.iscpif.file, partners), href := shared.link.partner.iscpif, target := "_blank")
    a(img(src := Resource.img.partner.geocite.file, partners), href := shared.link.partner.parisgeo, target := "_blank")
    a(img(src := Resource.img.partner.biomedia.file, partners), href := shared.link.partner.biomedia, target := "_blank")
    a(img(src := Resource.img.partner.idf.file, partners), href := shared.link.partner.idf, target := "_blank")
    a(img(src := Resource.img.partner.paris.file, partners), href := shared.link.partner.paris, target := "_blank")
    a(img(src := Resource.img.partner.trempoline.file, partners), href := shared.link.partner.trempoline, target := "_blank")
  }
}

${h2{"Some scientific collaborations outputs"}}

${aa(img(src := Resource.img.partner.ign.file, smallPartners), href := shared.link.partner.ign, float:="left")}
${span(width := "75%")}


${h3{"Simplu3D-PSE"}}

Exploration of the output diversity of the ${aa("SimPLU3D model", href := shared.link.repo.simplu)} via the PSE method.
Simplu3D is a model that simulates the building and the placement of buildings, abiding by the Right To Build documents of the French legislation: the PLU (Plan Local d'Urbanisme).
The PSE exploration method led to this decision making tool: ${outerLink(shared.link.simpluDemo.split("//").last, shared.link.simpluDemo)} enabling user to explore the effects of the PLU rules variations on the resulting morphology of the urban islet.


${aa(img(src := Resource.img.partner.geocite.file, smallPartners), href := shared.link.partner.parisgeo, float:="left")}
${span(width := "75%")}


${h3{"SimPopLocal"}}
Theoretical geography model, simulating the emergence and the upkeep of systems of settlements (ancestors of system of cities) by means of spatial interactions and innovation exchanges.
Main results are shown in the «Half a billion simulations: Evolutionary algorithms and distributed computing for calibrating the SimpopLocal geographical model» article in our  ${a("publication page", href := DocumentationPages.communications.file)}.


${span(width := "75%")}


${h3{"MARIUS"}}

${h6{"\"Modélisation des Agglomérations de Russie Imperiale et ex-Union Soviétique\", French for \"Former USSR agglomerations model\""}}
It is the last born of the Simpop model family, and addresses the dynamics of Russia/former USSR from 1950 to 2010.
See «A modular modelling framework for hypotheses testing in the simulation of urbanisation», in our ${a("publication page", href := DocumentationPages.communications.file)}.


${span(width := "75%")}


${h3{"Geodivercity"}}

The ERC ${aa("Geodivercity", href:=shared.link.geodivercity)} was an advanced grant ERC project, led by Pr. Denise Pumain.
A significant part of the  development of OpenMOLE  as a scalable platform for geographical simulation models experiments was founded by this project.
The project led to the publication of a book published by Springer, available ${aa("on the editor site", href:=shared.link.ercSpringer)} and as a ${aa("[pre-print version]" , href:=Resource.paper.urbanDynamics.file)}

${h2{"OpenMOLE advocates"}}

They actively participate in the dissemination and training of OpenMOLE users around the world:
${ul(
  li{"François Lavallée - INRAE Paris, France"},
  li{"Srirama Bhamidipati - Delft University of Technology, Netherlands"}
)}
""")