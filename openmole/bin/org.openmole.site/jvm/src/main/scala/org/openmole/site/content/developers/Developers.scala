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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._

object Developers extends PageContent(html"""

In this section you will find information on:
${ul(
  li{html"How to use OpenMOLE in interactive ${a("console mode", href := console.file)}"},
  li{html"How to develop your own ${a("plugin", href := pluginDevelopment.file)} to have full control over your code, and how to integrate it in OpenMOLE"},
  li{html"How to develop an OpenMOLE extension ${a("extension", href := extensionAPI.file)}"},
  li{html"How to use the OpenMOLE ${a("REST API", href := restAPI.file)}"},
  li{html"How to compile and modify the ${a("documentation", href := documentationGen.file)}"}
)}

""")
