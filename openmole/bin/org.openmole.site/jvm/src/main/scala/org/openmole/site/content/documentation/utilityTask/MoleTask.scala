package org.openmole.site.content.documentation.utilityTask

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

object MoleTask extends PageContent(html"""

A ${i{"MoleTask"}} encapsulates a whole workflow in a single task. It may be useful to delegate entire workflow executions to a remote node. Another typical use case is to make your workflows more modular.

$br

To encapsulate a workflow in a ${i{"MoleTask"}}, simply wrap it in the ${i{"MoleTask"}} builder:

$br

${hl.openmole("""
// Define tasks t1 and t2
val moleTask = MoleTask(t1 -- t2)

// Delegate the whole workflow execution to an execution environment
moleTask on env
""", header = """
val t1 = EmptyTask()
val t2 = EmptyTask()
val env = LocalEnvironment()""")}

$br

In that case, the ${i{"MoleTask"}}'s inputs are the same as ${i{"t1"}}'s and its outputs are the same as ${i{"t2"}}'s.

""")
