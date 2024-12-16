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

object SSH extends PageContent(html"""
The ${code{"SSHEnvironment"}} makes it possible to delegate tasks to a remote multi-core server through an ${aa("SSH", href := shared.link.ssh)} connection.

$br$br

You should first provide your ${aa("authentication", href := DocumentationPages.gui.file + "#Authentications")} information to OpenMOLE to be able to use your batch system.
Then, all that is left is to define is the environment itself.
Here is a use example:

$br$br

${hl.openmole("""
  val env =
    SSHEnvironment(
      "login",
      "machine.domain",
      10 // The number of cores you want to use on the remote server
    )
""")}

$br

$provideOptions:
${ul(
  li{html"$port,"},
  li{html"$openMOLEMemory,"},
  li{html"$threads,"},
  li{html"$workDirectory,"},
  li{html"${code{"killAfter"}}: use the timeout command to kill the process after a given time, for exemple killAfter = 24 hours,"},
  li{html"$reconnect"}
)}

""")


