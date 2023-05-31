package org.openmole.site.content.scale

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
import Environment.*


object Multithread extends PageContent(html"""

By default OpenMOLE tasks run on a single thread of your local machine.
To use several cores of your computer, you can define a so called ${code{"LocalEnvironment"}} with the number of threads to use as a parameter, see below:

$br$br

${hl.openmole("""
// Use 10 threads / cores
val env = LocalEnvironment(10)
""")}

""")


