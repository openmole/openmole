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
import org.openmole.site.content.scale.Environment.*

object MiniClust extends PageContent(html"""
${aa("MiniClust", href := "https://github.com/openmole/miniclust")} is a lightweight and easy to deploy distributed execution environment that is well fitted for OpenMOLE.

The ${code{"MiniclustEnvironment"}} makes it possible to delegate job a MiniClust cluster.

$br$br

You should first provide your ${aa("authentication", href := DocumentationPages.gui.file + "#Authentications")} information to OpenMOLE to be able to use your batch system.
Then, all that is left is to define is the environment itself.

Here is a use example:

$br$br

${hl.openmole("""
  val env =
    MiniclustEnvironment(
      "login",
      "https://babar.openmole.org"
    )
""")}

$br

$provideOptions:
${ul(
  li{html"$openMOLEMemory,"},
  li{html"$runtimeSetting,"},
  li{html"${apiEntryTitle{"core"}}, the number of cores used by each job"},
  li{html"${apiEntryTitle{"time"}}, the maximum amount of time after which the job gets killed, the default on MiniClust is 1 hour"},
  li{html"${apiEntryTitle{"insecure"}}, true if the server provides an insecure https connection"}
)}

""")


