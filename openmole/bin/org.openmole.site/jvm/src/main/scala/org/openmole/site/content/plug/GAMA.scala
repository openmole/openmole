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
    outputs += (numberOfPreys mapped "nb_preys")
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
    li(html"""${code{"project"}} File, the location of your GAMA project directory, $mandatory, for instance ${code{"project = workDirectory / \"gamaproject\""}}"""),
    li(html"""${code{"gaml"}} String, the relative path of your ${i{".gaml"}} file in your work directory, $mandatory, for instance ${code{"gaml = \"model/model.gaml\""}}"""),
    li(html"""${code{"finalStep"}} Int, the last simulation step of you simulation, it must be set if no stopining condtion is set"""),
    li(html"""${code{"stop"}} String, a stoping condition for your simulation in GAMA, for instance ${code("(cycle=10 or nb_preys<10)")}, it must be set if no finalStep is set, in case both are set only the stopping condition in taken into accoount"""),
    li(html"""${code{"seed"}} Long, the OpenMOLE variable used to set the GAMA random number generator seed, $optional the seed is randomly drawn if not set"""),
    li{html"${code{"version"}} String, $optional. The version of GAMA to run."},
    li(html"""${code{"containerImage"}} the label of a container image or a container file containing GAMA headless, $optional, the default value is "gamaplatform/gama:1.9.0""""),
    li(html"""${code{"memory"}} the memory allocated to the gama headless, $optional, for example ${code{"memory = 3000 megabytes"}}"""),
    li(html"""${code{"install"}} some command to run on the host system to initialise the container, for instance ${code("""Seq("apt update", "apt install mylib")""")}, $optional"""),
)}


${h2("Reproduce an error using GAMA headless")}

The integration of GAMA into OpenMOLE is achieved through a container. OpenMOLE downloads the container for the version you're interested in and use it to run your simulation. Sometimes, issues can arise in the communication between these two tools.

To get a clearer understanding, it's important to determine which of the two is causing the problem. Here, using the predator-prey model as a basis, we suggest testing your model within the virtual machine that will be used by OpenMole. This allows you to perform diagnostics in case of a failure.

OpenMOLE communicates with GAMA by defining an experiment and then running the batch mode of 'gama-headless.sh'. Therefore, to run GAMA in headless mode, you need to:
${ul(
    li(html"write an experiment file, that import you model,"),
    li(html"launch the simulation using this experiment.")
  )}

In case of an error, OpenMOLE should display the experiment that was generated.

Here is an example:
${code(
    """model openmoleexplorationmodel
      |
      |import 'mymodel.gaml'
      |
      |experiment _openMOLEExperiment_ {
      |
      |  float seed <- 42;
      |  //Set some parameters
      |  parameter var:nb_preys_init <- 100;
      |
      | reflex stop_reflex when:cycle=10 {
      |
      |   // Some outputs to save in the json file
      |    map _outputs_ <- [
      |    "nb_preys"::nb_preys
      |   ];
      |
      | save to_json(_outputs_) to:"om_output.json" format:"txt";
      |
      | do die;
      | }
      |}
      |""".stripMargin)}


Modify it to match your model and save it as 'experiment.gaml'. Make sure the import path is coherent with the location of you model relative to the location of the experiment file.

${h3("Using a existing GAMA installation")}

You can run:
${code("""
./gama-headless.sh -batch _openMOLEExperiment_ experiment.gaml
""")}

Once the headless has run, you should find a file named ${i("om_output.json")}. Check that it exists, is well formed and contains the simulation results. Otherwise you can check you model or experiment for errors. If you find a bug in GAMA, you can report it to the GAMA developers on ${aa("Github", href := "https://github.com/gama-platform/gama")}.

${h3("Using Docker")}

To reproduce a behaviour close from what OpenMOLE executes you can run the GAMA docker image. You should have docker installed on your computer.

$br

Download the Gama Docker image. All available containers can be found on the ${aa("docker hub", href := "https://hub.docker.com/r/gamaplatform/gama")}.

Here, we will use the alpha version.

${code("""
export GAMA_VERSION="alpha" # Replace by the version you want to test
docker pull gamaplatform/gama:$GAMA_VERSION
""")}

To get an interactive shell into the GAMA docker:
${code("""
docker run -it -v "/tmp/gama model/":/work --entrypoint  /bin/bash gamaplatform/gama:$GAMA_VERSION
""")}

${i("-it")} stands for interactive terminal. ${i("-v \"/tmp/gama model/\":/work")} mounts is a volume between your host system and the container.
${i("/tmp/gama model/")} is a folder on your computer. ${i("/work")} is the folder inside the container. This mount allows the container to access the model files.

$br

Once inside the Docker container, you're in the ${i("/opt/gama-platform/headless")} directory, and the model is in the folder ${i("/work")}.
You can then run GAMA commands to run your model.

${code(
    """
      |./gama-headless.sh -batch _openMOLEExperiment_ /work/experiment.gaml""".stripMargin)}

Once the headless has run, you should find a file named ${i("om_output.json")}. Check that it exists, is well formed and contains the simulation results. Otherwise, you can check your model for error. If you find a bug in GAMA, you can report it to the GAMA developers on ${aa("Github", href := "https://github.com/gama-platform/gama")}.
""")
