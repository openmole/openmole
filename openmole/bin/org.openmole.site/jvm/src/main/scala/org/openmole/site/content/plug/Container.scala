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

object Container extends PageContent(html"""
To call a native executable in an OpenMOLE task, you can either use:
${ul(
   li{html"the ${code{"ContainerTask"}} that runs your executable in a portable container,"},
   li{html"or the ${code{"SystemExecTask"}} that executes a local command line as if you where in a terminal."}
  )}

${h2{"Execute Your Code In a Containers"}}

The ${code{"ContainerTask"}} runs ${a(href := "https://www.docker.com/resources/what-container", "docker containers")} in OpenMOLE. When your model is written in a language for which a specific OpenMOLE task doesn't exist, or if it uses an assembly of tools, libraries, binaries, etc. you might want to use a container task to make it portable, so that you can send it to any machine (with potentially varying OS installations).

${h3{"Preliminary remarks"}}

${Native.preliminary("ContainerTask")}

${h3{"Containers from the docker hub"}}

A simple task running a Python container would look like:

$br$br

${hl.openmole(s"""
  val container = ContainerTask(
    "python:3.6-stretch",
    ${tq}python -c 'print("splendid!")'${tq}
  )
""")}

$br

You can run this task.
At launch time, it downloads the Python image from the docker hub in order to be able to run it afterwards.

$br$br

Let's imagine a slightly more complete example: we will use the following Python script, which uses the ${i{"numpy"}} library to multiply a matrix (stored in a csv file) by a scalar number.

$br$br


${hl("""
import sys
import numpy
from numpy import *
from array import *
import csv

input = open(sys.argv[1],'r')
n = float(sys.argv[2])

print("reading the matrix")
data = csv.reader(input)
headers = next(data, None)

array = numpy.array(list(data)).astype(float)

print(array)
print(n)
mult = array * n

print("saving the matrix")
numpy.savetxt(sys.argv[3], mult, fmt='%g')
""", "python")}

$br

An example input file would look like this:

$br$br



${hl("""
col1,col2,col3
31,82,80
4,48,7
""", "csv")}

$br

For this example, we consider that we have a ${i{"data"}} directory, containing a set of csv files in the ${code{"workDirectory"}}.
We want to compute the Python script for each of this csv file and for a set of values for the second argument of the Python script.
The OpenMOLE workflow would then look like this:

$br$br



${hl.openmole(s"""
  // Declare variables
  val dataFile = Val[File]
  val dataFileName = Val[String]
  val i = Val[Int]
  val resultFile = Val[File]

  // Task
  val container = ContainerTask(
    "python:3.6-stretch",
    ${tq}python matrix.py data.csv $${i} out.csv${tq},
    install = Seq("pip install numpy")
  ) set (
    resources += workDirectory / "matrix.py",
    inputFiles += (dataFile, "data.csv"),
    outputFiles += ("out.csv", resultFile),
    (inputs, outputs) += (i, dataFileName)
  )

  // Workflow
  DirectSampling(
    sampling =
      (dataFile in (workDirectory / "data") withName dataFileName) x
        (i in (0 to 3)),
    evaluation = container
  ) hook (workDirectory / "result")
""")}

$br

The ${code{"install"}} parameter contains a set of command used to install some components in the container once and for all, when the task is instantiated.

${h3{"Containers and archive"}}

You can also craft your own docker image on your machine, export it and then run it using the ${code{"ContainerTask"}}.

$br

For instance lets consider the following ${i{"DockerFile"}}:
${code("""FROM debian:testing-slim
RUN apt update && apt install -y fortune && apt clean""")}

$br

You can build the container using docker:
${plain("""[reuillon:/tmp/test] 19h25m27s $ docker build . -t mycontainer
Sending build context to Docker daemon  2.048kB
Step 1/2 : FROM debian:testing-slim
testing-slim: Pulling from library/debian
09da4619dc13: Pull complete
Digest: sha256:fa46229fd1d1acc27c9ff99960abba8f3c7a29073767906e4f935a80f2f4dbef
Status: Downloaded newer image for debian:testing-slim
 ---> 282aee73ba41
Step 2/2 : RUN apt update && apt install -y fortune && apt clean
 ---> Running in f1b20c2572d8
[...]
The following NEW packages will be installed:
  fortune-mod fortunes-min librecode0
0 upgraded, 3 newly installed, 0 to remove and 20 not upgraded.
[...]
Removing intermediate container f1b20c2572d8
 ---> bae4a6b38b4a
Successfully built bae4a6b38b4a
Successfully tagged mycontainer:latest""")}

$br

Then save you container in an archive:
${plain("""docker save mycontainer -o /tmp/container.tar""")}

$br

You can now upload this archive in your OpenMOLE work directory and run it with a ${code{"ContainerTask"}}:

$br$br

${hl.openmole(s"""
val result = Val[String]

val container = ContainerTask(
  workDirectory / "container.tar",
  "/usr/games/fortune",
  stdOut = result
)

val upper =
  ScalaTask("result = result.toUpperCase") set(
    (inputs, outputs) += result
  )

container -- (upper hook display)
""")}


${h3{"Advanced Options"}}

${h6{"Return value"}}
Some applications disregarding standards might not return the expected 0 value upon completion.
The return value of the application is used by OpenMOLE to determine whether the task has been successfully executed, or needs to be re-executed.
Setting the boolean flag ${code("errorOnReturnValue")} to ${code{"false"}} will prevent OpenMOLE from re-scheduling a ${i{"ContainerTask"}} that has reported a return code different from 0.
You can also get the return code in a variable using the ${code("returnValue")} setting.
In case you set this option, a return code different from 0 won't be considered an error and the task produce an output with the value of the return code.

$br$br

${hl.openmole(s"""
  // Declare variable
  val ret = Val[Int]

  // Task
  val container = ContainerTask(
    "python:3.6-stretch",
    ${tq}python matrix.py${tq},
    returnValue = ret
  )

  // Workflow
  container hook display
""")}

${h6{"Standard and error outputs"}}
Another default behaviour is to print the standard and error outputs of each task in the OpenMOLE console.
Some processes might display useful results on these outputs.
A ${code{"ContainerTask"}}'s standard and error outputs can be set to OpenMOLE variables and thus injected in the data flow by summoning respectively the ${code("stdOut")} and ${code("stdErr")} actions on the task.

$br$br

${hl.openmole(s"""
    // Declare variable
    val myOut = Val[String]

    // Tasks
    val container = ContainerTask(
      "debian:testing-slim",
      ${tq}echo 'great !'${tq},
      stdOut = myOut
    )

    val parse = ScalaTask(${tq}myOut = myOut.toUpperCase${tq}) set (
      (inputs, outputs) += myOut
    )

    // Workflow
    container -- (parse hook display)
""")}

${h6{"Environment variables"}}
As any other process, the applications contained in OpenMOLE's native tasks accept environment variables to influence their behaviour.
Variables from the data flow can be injected as environment variables using the ${code{"environmentVariable"}} parameter.
If no name is specified, the environment variable is named after the OpenMOLE variable.
Environment variables injected from the data flow are inserted in the pre-existing set of environment variables from the execution host.
This shows particularly useful to preserve the behaviour of some toolkits when executed on local environments (ssh, clusters...) where users control their work environment.

$br$br

${hl.openmole(s"""
    // Declare variable
    val name = Val[String]

    // Task
    val container = ContainerTask(
      "debian:testing-slim",
      ${tq}env${tq},
      environmentVariables = Seq("NAME" -> "$${name.toUpperCase}")
    ) set (inputs += name)

    // Workflow
    DirectSampling(
      sampling = name in List("Bender", "Fry", "Leila"),
      evaluation = container
    )
""")}

${h6{"Using local resources"}}
To access data present on the execution node (outside the container filesystem) you should use a dedicated option of the ${code{"ContainerTask"}}: ${code{"hostFiles"}}.
This option takes the path of a file on the execution host and binds it to the same path in the container filesystem.

$br$br

${hl.openmole(s"""
    ContainerTask(
      "debian:testing-slim",
      ${tq}ls /bind${tq},
      hostFiles = Seq("/tmp" -> "/bind")
    )
""")}

${h6{"WorkDirectory"}}
You may set the directory within the container where to start the execution from.

$br$br

${hl.openmole(s"""
    ContainerTask(
      "debian:testing-slim",
      ${tq}ls${tq},
      workDirectory = "/bin"
    )
""")}



${h2{"Execute Any Program Available on the Machine"}}

The ${code{"ContainerTask"}} was designed to be portable from one machine to another.
However, some use-cases require executing specific commands installed on a given cluster.
To achieve that you should use another task called ${code{"SystemExecTask"}}.
This task is made to launch native commands on the execution host.
There is two modes for using this task:

${ul(
  li(html"Calling a command that is assumed to be available on any execution node of the environment. The command will be looked for in the system as it would from a traditional command line: searching in the default ${i{"PATH"}} or an absolute location."),
  li(html"Copying a local script not installed on the remote environment. Applications and scripts can be copied to the task's work directory using the ${code{"resources"}} field. Please note that contrary to the ${code{"ContainerTask"}}, there is no guarantee that an application passed as a resource to a ${code{"SystemExecTask"}} will re-execute successfully on a remote environment."),
)}

$br

The ${code{"SystemExecTask"}} accepts an arbitrary number of commands.
These commands will be executed sequentially on the same execution node where the task is instantiated.
In other words, it is not possible to split the execution of multiple commands grouped in the same ${code{"SystemExecTask"}}.

$br$br

The following example first copies and runs a bash script on the remote host, before calling the remote's host ${code{"/bin/hostname"}}.
Both commands' standard and error outputs are gathered and concatenated to a single OpenMOLE variable, respectively ${code{"stdOut"}} and ${code{"stdErr"}}:

$br$br

${hl.openmole("""
    // Declare variables
    val output = Val[String]
    val error  = Val[String]

    // Task
    val scriptTask =
      SystemExecTask(
        command = Seq("bash script.sh", "hostname"),
        stdOut = output,
        stdErr = error
      )

    // Workflow
    scriptTask hook display
""")}

$br

In this case the bash script might depend on applications installed on the remote host.
Similarly, we assume the presence of ${code{"/bin/hostname"}} on the execution node.
Therefore this task cannot be considered as portable.

$br

Note that each execution is isolated in a separate folder on the execution host and that the task execution is considered as failed if the script returns a value different from 0.
If you need another behaviour you can use the same advanced options as the ${code{"ContainerTask"}} regarding the return code.

${h3{"File management"}}

To provide files as input of a ${code{"ContainerTask"}} or ${code{"SystemExecTask"}} and to get files produced by these tasks, you should use the ${code{"inputFiles"}} and ${code{"outputFiles"}} keywords.
See the ${a("documentation on file management", href := DocumentationPages.fileManagement.file)}.


${h2{"Generate Complex Parameter Files"}}

To generate complex input file for you model from OpenMOLE variable, you might want to use the ${a("TemplateFileTask", href := DocumentationPages.templateTask.file)}.

""")


