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

object Publications extends PageContent(html"""
${h2{"How to cite OpenMOLE?"}}

${h3{"Whenever you're using the platform"}}

${ul(
  li(html"""
    Romain Reuillon, Mathieu Leclaire, and Sebastien Rey-Coyrehourcq,
    « OpenMOLE, a workflow engine specifically tailored for the distributed exploration of simulation models »,
    published in ${i{"Future Generation Computer Systems"}}, 2013.

    $br

    ${aa("[editor version]" , href := shared.link.paper.fgcs2013)}
    ${aa("[online preprint]" , href := shared.link.paper.fgcs2013preprint)}
    ${aa("[bibteX]", href := Resource.bibtex.FGCSRefBib.file)}
    ${aa("[pdf]", href :=Resource.paper.fgcs2013.file)}"""),

  li(html"""
    Romain Reuillon, Florent Chuffart, Mathieu Leclaire, Thierry Faure, Nicolas Dumoulin, and  David R.C. Hill,
    « Declarative Task Delegation in OpenMOLE »,
    proceedings of ${i{"High Performance Computing and Simulation (HPCS)"}} international conference, 2010.

    $br

    ${aa("[editor version]" , href := shared.link.paper.hpcs2010)}
    ${aa("[bibteX]", href := Resource.bibtex.HPCSRefBib.file)}
    ${aa("[pdf]", href := Resource.paper.hpcs2010.file)}""")
)}

${h3(html"""When you're using the ${a("reproducibility features", href := DocumentationPages.container.file)}""")}

${ul(li(Papers.frontiers2017))}

${h2{"Scientific papers about OpenMOLE methods"}}

${ul(
  li(html"""
    Romain Reuillon, Clara Schmitt, Ricardo De Aldama, and Jean-Baptiste Mouret,
    « A New Method to Evaluate Simulation Models: The Calibration Profile (CP) Algorithm »,
    published in @i{Journal of Artificial Societies and Social Simulation} (JASSS) , Vol 18, Issue 1, 2015.

    $br

    ${aa("[online version]" , href := shared.link.paper.jassCP)}
    ${aa("[bibteX]", href := Resource.bibtex.profilemethodBib.file)}"""),

  li(html"""
    Guillaume Chérel, Clémentine Cottineau, and Romain Reuillon,
    « Beyond Corroboration: Strengthening Model Validation by Looking for Unexpected Patterns »,
    published in ${i{"PLOS ONE"}} 10(9), 2015.

    $br

    ${aa("[online version]" , href := shared.link.paper.beyondCorroboration)}
    ${aa("[bibteX]", href := Resource.bibtex.PSEmethodBib.file)}""")
)}


${h2{"(a selection of) Scientific papers/conference proceedings using OpenMOLE features}"}}

${ul(
  li(html"""
    Juste Raimbault, Joris Broere, Marius Somveille, Jesus Mario Serna, Evelyn Strombom, Christine Moore, Ben Zhu, and Lorraine Sugar,
    « A spatial agent-based model for simulating and optimizing networked eco-industrial systems »,
    published in ${i{"Resources, Conservation and Recycling"}}, 155, 104538, 2020.

    $br

    ${aa("[paper]", href := shared.link.paper.rcr2020)}"""),

  li(html"""
    Juste Raimbault, Clémentine Cottineau, Marion Le Texier, Florent Le Néchet, and Romain Reuillon,
    « Space Matters: Extending Sensitivity Analysis to Initial Spatial Conditions in Geosimulation Models »,
    published in ${i{"Journal of Artificial Societies and Social Simulation"}}, 22(4): 10, 2019.

    $br

    ${aa("[paper]", href := shared.link.paper.jasss2019)}"""),

  li(html"""
    Juste Raimbault,
    « Calibration of a density-based model of urban morphogenesis »,
    published in ${i{"PLOS ONE"}}, 13(9): e0203516, 2018.

    $br

     ${aa("[paper]", href := shared.link.paper.pone2018)}"""),

  li(html"""
    Juste Raimbault,
    « Indirect evidence of network effects in a system of cities »,
    published in ${i{"Environment and Planning B: Urban Analytics and City Science"}}, 2399808318774335, 2018.

    $br

    ${aa("[open-access version]", href := shared.link.paper.epb2018arxiv)}
    ${aa("[editor version]", href := shared.link.paper.epb2018)}"""),

  li(html"""
    Denise Pumain, Romain Reuillon, Sébastien Rey-Coyrehourcq, Arnaud Banos, Paul Chapron, et al..
    « Urban Dynamics and Simulation Models »,
    published by Springer, ISBN 978-3-319-46495-4, 2017.

    $br

    ${aa("[pre-print version]", href :=Resource.paper.urbanDynamics.file)}
    ${aa("[bibteX]", href := Resource.bibtex.urbanDynamicsBib.file)}"""),

  li(html"""
    Jonathan Passerat-Palmbach, Romain Reuillon, Mathieu Leclaire, Antonios Makropoulos, Emma C. Robinson, Sarah Parisot, and Daniel Rueckert,
    « Reproducible Large-Scale Neuroimaging Studies with the OpenMOLE Workflow Management System »,
    published in ${i{"Frontiers in Neuroinformatics"}}, Vol 11, 2017.

    $br

    ${aa("[online version]" , href := shared.link.paper.frontier2017)}
    ${aa("[bibteX]", href := Resource.bibtex.frontierBib.file)}"""),

  li(html"""
    Clara Schmitt, Sébastien Rey-Coyrehourcq, Romain Reuillon, and Denise Pumain,
    « Half a billion simulations: Evolutionary algorithms and distributed computing for calibrating the SimpopLocal geographical model »,
    published in @i{Environment and Planning B: Urban Analytics and City Science}, Vol 42, Issue 2, 2015.

    $br

    ${aa("[open-access version]", href := shared.link.paper.halfBillionOA)}
    ${aa("[editor version]", href := shared.link.paper.halfBillionEditor)}
    ${aa("[bibteX]", href := Resource.bibtex.halfbillionBib.file)}"""),

  li(html"""
    Clémentine Cottineau, Paul Chapron, and Romain Reuillon,
    « Growing models from the bottom up. An evaluation-based incremental modelling method (EBIMM) applied to the simulation of systems of cities »,
    published in ${i{"Journal of Artificial Societies and Social Simulation"}} (JASSS) , Vol 18, Issue 4, 2015.

    $br

    ${aa("[online version]", href := shared.link.paper.jass2015)}
    ${aa("[bibteX]", href := Resource.bibtex.EBIMMBib.file)}"""),

  li(html"""
    Clémentine Cottineau, Romain Reuillon, Paul Chapron, Sébastien Rey-Coyrehourcq, and Denise Pumain,
    « A modular modelling framework for hypotheses testing in the simulation of urbanisation »,
    published in @i{Systems}, Vol 3, Issue 4, 2015.

    $br

    ${aa("[open-access version]", href := shared.link.paper.mdpi2015)}
    ${aa("[bibteX]", href := Resource.bibtex.EBIMMBib.file)}""")
)}
""")

