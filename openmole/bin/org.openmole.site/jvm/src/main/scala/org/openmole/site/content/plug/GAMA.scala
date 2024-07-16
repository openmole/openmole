package org.openmole.site.content.plug

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

object GAMA extends PageContent(html"""

${h2{"Preliminary remarks"}}

${a("GAMA", href := "https://gama-platform.github.io/")} is a modelling and simulation development environment for building spatially explicit agent-based simulations.
OpenMOLE supports GAMA model natively through the ${code{"GAMATask"}}.

$br

${Native.preliminary("GAMATask")}

${h2{"The GAMATask"}}
${h3{"GAMA by example"}}

You can provide your ${i{".gaml"}} file to the ${code{"GAMATask"}} to run your model and explore it with OpenMOLE.
The example below illustrates an exploration of the predator-prey model of the GAMA model library using a direct sampling:

$br$br

${hl.openmole("""
// Declare the variables
val numberOfPreys = Val[Double]
val nbPreysInit = Val[Int]
val mySeed = Val[Long]

// Gama task
// The first argument is the project directory
// The second argument is the relative path of the gaml file in the project directory
// The second argument is the Gama experiment name
// The third argument is the number of steps
val gama =
  GAMATask(project = workDirectory / "predator", gaml = "predatorPrey.gaml", experiment = "prey_predatorExp", finalStep = 100, seed = mySeed) set (
    inputs += (nbPreysInit mapped "nb_preys_init"),
    outputs += (numberOfPreys mapped "Number of preys")
  )

// Explore and replicate the model
DirectSampling(
  evaluation =
    Replication(
      evaluation = gama,
      seed = mySeed,
      sample = 10,
      aggregation = Seq(numberOfPreys evaluate average)) hook(workDirectory / "result"),
  sampling = nbPreysInit in (0 to 200 by 50)
) hook display
""")}


${h3{"Task arguments"}}

The GAMA task uses the following arguments:
${ul(
    li(html"""${code{"project"}} the location of your GAMA project directory, $mandatory, for instance ${code{"project = workDirectory / \"gamaproject\""}}"""),
    li(html"""${code{"gaml"}} the relative path of your ${i{".gaml"}} file in your work directory, $mandatory, for instance ${code{"gaml = \"model/model.gaml\""}}"""),
    li(html"""${code{"experiment"}} the name of your experiment as implemented in the ${i{".gaml"}} file, $mandatory"""),
    li(html"""${code{"finalStep"}} the last simulation step of you simulation, $mandatory"""),
    li(html"""${code{"frameRate"}} the frame rate to sample you simulation dynamics, in this case the outputs should be arrays, $optional"""),
    li(html"""${code{"seed"}} the OpenMOLE variable used to set the GAMA random number generator seed, $optional the seed is randomly drawn if not set"""),
    li(html"""${code{"containerImage"}} the label of a container image or a container file containing GAMA headless, $optional, the default value is "gamaplatform/gama:1.9.0""""),
    li(html"""${code{"memory"}} the memory allocated to the gama headless, $optional, for example ${code{"memory = 3000 megabytes"}}"""),
    li(html"""${code{"install"}} some command to run on the host system to initialise the container, for instance ${code("""Seq("apt update", "apt install mylib")""")}, $optional"""),
)}
""")
