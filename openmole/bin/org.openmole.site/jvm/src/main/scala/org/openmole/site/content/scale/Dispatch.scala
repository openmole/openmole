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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, name => _, _}
import org.openmole.site._
import org.openmole.site.tools.*
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Environment.*


object Dispatch extends PageContent(html"""

Environment can be pooled together using the dispatch environment. Each environment provided to the dispatch environment are assigned with a number of slot. The dispatch environment maintains the number of jobs matching the given number of slots on each environment.

$br$br

${hl.openmole("""
val local = LocalEnvironment(2)
val ssh = SSHEnvironment("login", "machine.domain", 5)
val cluster = SLURMEnvironment("login", "machine.domain")

val dispatch =
  DispatchEnvironment(
    slot = Seq(
      4 on local,
      10 on ssh,
      100 on cluster
    )
  )
""")}

""")


