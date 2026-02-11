package org.openmole.site.content.download

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

object ReleaseNotesValue:
  lazy val closedIssues = Map(
    "12" -> "https://github.com/openmole/openmole/milestone/10",
    "10" -> "https://github.com/openmole/openmole/milestone/8",
    "9" -> "https://github.com/openmole/openmole/milestone/7?closed=1",
    "8" -> "https://github.com/openmole/openmole/milestone/4?closed=1",
    "7" → "https://github.com/openmole/openmole/milestone/3?closed=1",
    "6.2" → "",
    "6.1" → "https://github.com/openmole/openmole/milestone/6?closed=1",
    "6.0" → "https://github.com/openmole/openmole/milestone/2?closed=1"
  )

import ReleaseNotesValue.*

object ReleaseNotes extends PageContent(html"""

${h2{"V22 | Cranky Crocodile"}}

${
  notes("""
    - Use GAMA batch mode in GAMATask instead of legacy mode
    - Optimise OMR json reading
    - Fix Saltelli Sobol indices
    - Implement sampling reading from an OMR file
    - Allow to combine minus syntax and evaluate in evolution methods
    - Reduce launch time using ESBuild instead of Webpack
    - Reduce js size using ESBuild minification
    - Integrate Netlogo 7
    - Update container version (Python, Julia, Scilab)
    - Resources embedding in SIF container files to avoid archive extraction
    - Display of staging in GUI during the Preparing phase
    - Better isolate containers from the host system
    - Update Scala version to 3.8.1
    - Fix bugs
  """)
}

""")

