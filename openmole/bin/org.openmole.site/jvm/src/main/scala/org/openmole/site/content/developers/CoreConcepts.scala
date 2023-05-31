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

object CoreConcepts extends PageContent(html"""

This page gives a broad overview of stabilized concepts in OpenMOLE's core and its architecture.


${h1{"Workflow"}}

package ${code{"org.openmole.core.workflow"}}


${comment("builder : Lens, setter / getters, etc")}


${h2{"Composition"}}

${code{"org.openmole.core.workflow.composition"}} :
 - contains functions to construct the Puzzle, which contains Slots, Transitions and Capsules.
The puzzle is exported to an Execution for running the script.

 - contains the transitions and the corresponding DSL

 - contains the dsl linking transitions etc to puzzles


${comment("domain : traits for types for value domains for prototypes")}


""")
