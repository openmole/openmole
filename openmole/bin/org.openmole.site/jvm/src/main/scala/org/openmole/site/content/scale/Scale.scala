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


object Scale extends PageContent(html"""

A key feature in OpenMOLE is the possibility to delegate the workload to a remote execution environment.
Tasks in OpenMOLE have been designed so that delegating a part of the workload to a remote environment is declarative.



${h2{"Setting up an Authentication"}}

You first need to declare the environments you want to use and the corresponding authentication credentials, in the OpenMOLE GUI (see the ${aa("GUI guide", href := gui.file + "#Authentications")} for more information).
Have a look ${a("here", href := console.file + "#Authentications")} to set up an authentication in console mode.



${h2{"Defining an execution environment"}}

The actual delegation of the task is noted by the keyword ${code{"on"}} followed by a defined ${i{"Environment"}}:

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

// Declare a local environment using 10 cores of the local machine
val env = LocalEnvironment(10)

// Make the model run on the the local environment
DirectSampling(
  evaluation = model on env hook display,
  sampling = i in (0.0 to 100.0 by 1.0)
)
""")}

$br

You do not need to install anything or perform any kind of configuration on the target execution environment, OpenMOLE does all the work and uses the infrastructure in place.
You will however be required to provide the authentication information in order for OpenMOLE to access the remote environment (see ${a("here", href := "#SettingupanAuthentication")}).
In case you face authentication problems when targeting an environment through SSH, please refer to the corresponding entry in the ${aa("FAQ", href := faq.file + "#WhyismySSHauthenticationnotworking")}.

$br

When no specific environment is specified for a task or a group of tasks, they will be executed sequentially on your local machine.



${h2{"Grouping"}}

The use of a batch environment is generally not suited for short tasks (less than 1 minute for a cluster, or less than 1 hour for a grid).
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

// Declare a local environment using 10 cores of the local machine
val env = LocalEnvironment(10)

// Make the model run on the the local environment
DirectSampling(
  evaluation = model on env by 100 hook display,
  sampling = i in (0.0 to 1000.0 by 1.0)
)
""")}

${h2{"Available environments"}}

Multiple environments are available to delegate your workload, depending on the kind of resources you have at your disposal.
${ul(
  li{html"${a("Multi-thread", href := multithread.file)} permits to execute the tasks concurrently on your own machine,"},
  li{html"${a("SSH", href := ssh.file)} allows to execute tasks on a remote server, connecting through SSH,"},
  li{html"a wide variety of ${a("clusters", href := cluster.file)} are also available, such as ${aa("PBS/Torque", href := cluster.file +"#PBS")}, ${aa("SGE", href := cluster.file + "#SGE")}, ${aa("Slurm", href := cluster.file + "#Slurm")}, ${aa("Condor", href := cluster.file + "#Condor")}, or ${aa("OAR", href := cluster.file + "#OAR")},"},
  li{html"finally, you can also use the ${aa("European Grid Infrastructure", href := egi.file)} to execute tasks."}
)}

""")


