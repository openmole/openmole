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

object TemplateTask extends PageContent(html"""

${h2{"TemplateTask & TemplateFileTask"}}

The ${code("TemplateTask")} and the ${code("TemplateFileTask")} are designed to generate input files for a model, by replacing some part of a template using OpenMOLE variables. A template contains the file content and some OpenMOLE expressions withing ${code("${}")}.

$br

A ${code("TemplateTask")} usage looks as follow:
${hl.openmole(s"""
val x = Val[Int]
val parameterFile = Val[File]

val template =
  TemplateTask(
    $tq
    |param1 = 9
    |param2 = $${x}
    |$tq.stripMargin,
    parameterFile
  ) set (
    inputs += x
  )

val container =
  ContainerTask("ubuntu", "cat /tmp/parameter.txt") set (
    inputFiles += (parameterFile, "/tmp/parameter.txt")
  )

DirectSampling(
  evaluation = template -- container,
  sampling = x in (0 to 10)
)
""")}

$br

In this example the a file is generated for each value of x by the ${code("TemplateTask")}. This file is then provided to the task ${code("container")}.

$br

The ${code("TemplateFileTask")} get the template content from a file. For instance you could consider a file named ${code("template.json")}, containing some templated json:
${hl.json("""
{
  "quantity":            ${quantity},
  "minimumSize":         6,
  "maximumSize":         8
}
""")}

$br

You can then use the ${code("TemplateFileTask")} to generate the input file for you model, as follow:
${hl.openmole("""
val quantity = Val[Int]
val parameterFile = Val[File]

val template =
  TemplateFileTask(
    workDirectory / "template.json",
    parameterFile
  ) set (
    inputs += quantity
  )

val container =
  ContainerTask("ubuntu", "cat /tmp/parameter.json") set (
    inputFiles += (parameterFile, "/tmp/parameter.json")
  )

DirectSampling(
  evaluation = template -- container,
  sampling = quantity in (0 to 10)
)
""")}


""")
