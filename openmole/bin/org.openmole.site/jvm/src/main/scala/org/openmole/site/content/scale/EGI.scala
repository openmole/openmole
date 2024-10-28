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


object EGI extends PageContent(html"""

The ${aa("European Grid Infrastructure", href := shared.link.egi)} is a grid infrastructure gathering computing resources from all over the world.
It is a very powerful computing environment, but transpires as technically challenging to use.
OpenMOLE makes it very simple to benefit from the grid.

${h2{"Setting up an EGI authentication"}}

You first need to import your EGI certificate in OpenMOLE as described in the ${aa("GUI guide", href := DocumentationPages.gui.file + "#Authentications")}.


${h3{"Authentication in console mode"}}

${b{"!!! This is not recommended !!!"}}

$br$br

In the console, execute the following:

$br$br

${hl.openmole("""
EGIAuthentication() = P12Certificate(password, "/path/to/your/certificate.p12")
""", header = """def password = "" """)}

$br

You only need to execute this operation once.
OpenMOLE will store this information in your preferences folder.



${h2{"Submitting jobs to EGI"}}
${h3{"Mandatory parameter"}}

In order to use EGI you must be registered in a ${b{"Virtual Organisation"}} (VO).
The VO is the only compulsory parameter when creating an EGI environment within OpenMOLE.
In the following example the VO ${i{"biomed"}} is specified, but you can use any VO:

$br$br

${hl.openmole("""
  val env = EGIEnvironment("biomed")
""")}


${h3{"Optional parameters"}}

Other optional parameters are available when defining an ${code{"EGIEnvironment"}}:
${ul(
  li{html"${apiEntryTitle{"cpuTime"}} the maximum duration for the job in terms of CPU consumption, for instance 1 hour,"},
  li{html"$openMOLEMemory,"},
  li{html"${apiEntryTitle{"debug"}} generate debugging information about the execution node (hostname, date, memory, max number of file descriptors, user proxy, ...). Defaults to ${hl.openmoleNoTest{"debug = false"}}."}
)}

Here is a use example using these parameters:

$br$br

${hl.openmole("""
  val env =
    EGIEnvironment(
      "biomed",
      cpuTime = 4 hours,
      openMOLEMemory = 200 megabytes
    )
""")}


${h3{"Advanced parameters"}}

The ${code{"EGIEnvironment"}} also accepts a set of more advanced options:
${ul(
  li{html"${apiEntryTitle{"service"}} a DIRAC REST API,"},
  li{html"${apiEntryTitle{"group"}} the name of the DIRAC group,"},
  li{html"${apiEntryTitle{"bdii"}} the BDII to use for listing resources accessible from this VO. The BDII in your preference file is used, when this field is left unspecified."},
  li{html"${apiEntryTitle{"voms"}} the VOMS server used for the authentication,"},
  li{html"${apiEntryTitle{"fqan"}} additional flags for authentication,"},
  li{html"${apiEntryTitle{"setup"}} setup to use on the DIRAC server. It is set to \"Dirac-Production\" by default."}
)}

${h3{"Grouping"}}

You should also note that the use of a batch environment is generally not suited for short tasks, ${i{"i.e."}} less than 1 hour for a grid.
In case your tasks are short, you can group several executions with the keyword ${code{"by"}} in your workflow.
For instance, the workflow below groups the execution of ${b{"model"}} by 100 in each job submitted to the environment:

$br$br

${hl.openmole(s"""
// Define the variables that are transmitted between the tasks
val i = Val[Double]
val res = Val[Double]

// Define the model, here it is a simple task executing "res = i * 2", but it can be your model
val model =
  ScalaTask("val res = i * 2") set (
    inputs += i,
    outputs += (i, res)
  )

// Define a local environment
val env = LocalEnvironment(10)

// Make the model run on the the local environment
DirectSampling(
  evaluation = model on env by 100 hook display,
  sampling = i in (0.0 to 1000.0 by 1.0)
)
""")}
""")


