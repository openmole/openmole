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

object R extends PageContent(html"""

${h2{"RTask"}}

${aa("R", href := shared.link.rcran)} is a scripted language initially designed for statistics, but whose application range is much broader today (for example GIS, operational research, linear algebra, web applications, etc.), thanks to its large community and the variety of packages.
It may be convenient to use specific R libraries within a workflow, and therefore OpenMOLE provides a specific ${code{"RTask"}}.

${h3{"Preliminary remarks"}}

${Native.preliminary("RTask")}


${h3{"RTask syntax"}}

The ${code{"RTask"}} relies on an underlying ${code{"ContainerTask"}} but is designed to be transparent and takes only R-related arguments.
The current version of R used is ${shared.rTask.rVersion}.
It takes the following arguments :

${ul(
   li{html"${code{"script"}} String,$mandatory. The R script to be executed, either R code directly or a R script file."},
   li{html"${code{"libraries"}} Sequence of strings or tuple, $optional (default = empty). The name of R libraries that will be used by the script and need to be installed beforehand (note: as detailed below, installations are only done during the first execution of the R script, and then stored in a cached docker image). Dependencies for R libraries can be automatically resolved and installed, for that you can write (\"ggrah\", true) instead of \"ggraph\"."},
   li{html"${code{"clearContainerCache"}} Boolean, $optional (default = ${code{"false"}}). Should the R image and libraries be cleared and reinstalled (to ensure an update for example)? If ${code{"true"}}, the task will perform the installation (and thus the update) even if the library was already installed."}
)}

$br

The following properties must be defined using ${code{"set"}}:

${ul(
   li{html"${code{"input/output"}} similar to any other task,"},
   li{html"mapped input: the syntax ${code{"inputs += om-variable mapped \"r-variable\""}} establishes a link between the workflow variable ${code{"om-variable"}} (Val) and the corresponding R variable named ${code{"r-variable"}} (as a String). If variables have the same name, you can use the short syntax ${code{"inputs += my-variable.mapped"}},"},
   li{html"mapped output: similar syntax as inputs to collect outputs of the model."}
)}

$br

The following arguments are optional arguments for an advanced usage

${ul(
   li{html"${code{"install"}} Sequence of strings, $optional (default = empty). System commands to be executed prior to any R packages installation and R script execution. This can be used to install system packages using apt."},
   li{html"${code{"image"}} String, $optional (default = \"openmole/r2u:4.3.0\"). Changes the docker image used by the RTask. OpenMOLE uses ${a("r2u", href := "https://eddelbuettel.github.io/r2u/")}"},
   li{html"${code{"prepare"}} Sequence of strings, $optional (default = empty). System commands to be executed just before to the execution of R on the execution node."},
)}

Other advanced container options are also available to build the RTask (see ContainerTask): ${code{"errorOnReturnValue"}}, ${code{"returnValue"}}, ${code{"stdOut"}}, ${code{"stdErr"}}, ${code{"hostFiles"}}, ${code{"workDirectory"}}, ${code{"environmentVariables"}}, ${code{"containerSystem"}}, ${code{"installContainerSystem"}}.

$br

We develop below a detailed example of how to use a ${code{"RTask"}}, from a very simple use case to a more elaborate one, with system libraries and R libraries.


${h2{"Execute R code"}}

The toy R script for this first test case is the following:

$br$br

${hl("""
    # Define the function
    f <- function(x) {
        x + 1
    }

    # Use the function
    j <- f(2)
""", "R")}

$br

This script creates a function ${code{"f"}} that takes a parameter (a number) and adds 1 to it.
It then applies the function to the number 2.
We save this to a file named ${i{"myRScript.R"}} in our OpenMOLE workspace.


${h3{"Write R code in the RTask"}}

For our first example, we write the R script directly in the ${code{"RTask"}}.

$br$br

${hl.openmole(s"""
    // Declare variables
    val result = Val[Int]

    // Task
    val rTask1 = RTask($tq
        # Here you write your R code

        # Define the function
        f <- function(x) {
            x + 1
        }

        # Use the function
        j <- f(2)
    $tq) set (
        outputs += result mapped "j"
    )

    // Workflow
    rTask1 hook display
""")}

$br

We provide the ${code{"result"}} variable to store the result of the function execution ${code{"j"}}, and we display its value in the standard output through ${code{"hook display"}}.


${h3{"Running R code from a script"}}

Instead of writing the R code in the ${code{"RTask"}}, we can call an external R script containing the code to be executed.
We will use the file ${i{"myRScript.R"}} created earlier.
It needs to be uploaded in the OpenMOLE workspace.

${h6{"All the code is in the R script"}}
If all the R code you need is written in your R script, you just need to provide the path to this script.

$br$br

${hl.openmole( s"""
    // Declare variables
    val result = Val[Int]

    // Task
    val rTask2 = RTask(script = workDirectory / "myRScript.R") set (
        outputs += result mapped "j"
    )

    // Workflow
    rTask2 hook display
""")}

$br

This workflow should return the exact same result as the previous example.

${h6{"Additional R code is needed"}}

If you need additional R code besides what is included in your script, you need a mix of the first two examples.
We will need to write R code and thus use the syntax from the first example, while also providing an external R script.

$br

In order to use the R script, we need to use the ${code{"resources"}} field with the precise location of the file in our work directory.
It will then be imported in the @code{RTask} by the R primitive ${code{"source(\"myRScript.R\")"}}).

$br$br

${hl.openmole( s"""
    // Declare variables
    val result = Val[Int]

    // Task
    val rTask3 = RTask($tq
        # Import the external R script
        source("myRScript.R")

        # Add some code
        k <- j + 2
    $tq) set (
        resources += (workDirectory / "myRScript.R"),
        outputs += result mapped "k"
    )

    // Workflow
    rTask3 hook display
""")}

$br

This time, we modify the output of the R script (by adding 2 to the result) before returning a value to OpenMOLE.


${h2{"Provide input values"}}

We want to be able to define inputs to the ${code{"RTask"}} externally, and to store the output values.


${h3{"Mapped values"}}

It is possible to do so through the ${code{"inputs"}} and ${code{"outputs"}} parameters in the ${code{"set"}} part of the task.

$br$br

${hl.openmole( s"""
    // Declare variables
    val myInput = Val[Int]
    val myOutput = Val[Int]

    // Task
    val rTask4 = RTask($tq
        # Define the function
        f <- function(x) {
            x + 1
        }

        # Use the function
        j <- f(i)
    $tq) set (
        inputs += myInput mapped "i",
        outputs += myOutput mapped "j",

        // Default value for the input
        myInput := 3
    )

    // Workflow
    rTask4 hook display
""")}

$br

Here, ${code{"i"}} and ${code{"j"}} are R variables defined and used in the R code, while ${code{"myInput"}} and ${code{"myOutput"}} are OpenMOLE variables.
The syntax ${code{"om-variable mapped \"r-variable\""}} creates a link between the two, indicating that these should be considered the same in the workflow.

$br$br

If your OpenMOLE variable and R variable have the same name (say ${code{"my-variable"}} for instance), you can use the following shortcut syntax: ${code{"my-variable.mapped"}}.


${h3{"Combine mapped and classic inputs/outputs"}}

If you have several outputs, you can combine mapped outputs with classic outputs that are not part of the ${code{"RTask"}}:

$br$br

${hl.openmole( s"""
    // Declare variables
    val i = Val[Int]
    val j = Val[Double]
    val c = Val[Double] // c is not used in the RTask

    // Task
    val rTask5 =
      RTask($tq
        # Define the function
        f <- function(x) {
          x + 1
        }

        # Use the function
        j <- f(i)
      $tq) set (
        inputs += i.mapped,
        inputs += c,

        outputs += i, // i doesn't need to be mapped again, it was done just above
        outputs += j.mapped,
        outputs += c,

        // Default values
        i := 3,
        c := 2
      )

    // Workflow
    rTask5 hook display
""")}

$br

This technique can be used when you have a chain of tasks and you want to use a hook.
Indeed, the hook only captures outputs of the last executed task, thus we can add a variable of interest in the output of the task even if it does not appear in this task.


${h2{"Working with files"}}

It is possible to use files as arguments of a ${code{"RTask"}}.
The ${code{"inputFiles"}} keyword is used.
We emphasize that ${code{"inputFiles"}} is different from ${code{"resources"}}, which was used to import external R scripts.
${code{"inputFiles"}} is used to provide OpenMOLE variables of type ${code{"File"}} that can be acted upon in a workflow.

$br$br

In this example workflow, we first have a ${code{"ScalaTask"}} writing numbers in a file.
The file is created through the OpenMOLE variable ${code{"myFile"}} of type ${i{"java.io.File"}}.
In order to have access to this file in the ${code{"RTask"}}, we add ${code{"myFile"}} as an output of the ${code{"ScalaTask"}} and an input of the ${code{"RTask"}}.

$br$br

${hl.openmole(s"""
    // Declare variable
    val myFile = Val[File]
    val resR = Val[Array[Double]]

    // ScalaTask creating the file myFile
    val task1 = ScalaTask($tq
        val myFile = newFile()
        myFile.content = "3 6 4"
    $tq) set (
        outputs += myFile
    )

    // RTask using myFile as an input
    val rTask5 = RTask($tq
        myData <- read.table("fileForR.txt", sep = " ")
        myVector <- as.vector(myData, mode = "numeric")

        f <- function(x) {
            x + 1
        }

        k <- f(myVector)
    $tq) set(
        inputFiles += (myFile, "fileForR.txt"),
        outputs += resR mapped "k"
    )

    // Workflow
    task1 -- (rTask5 hook display)
""")}

$br

The R script in the ${code{"RTask"}} reads a file named ${i{"fileForR.txt"}} (in the R script presented here, it is supposed to have numeric values, separated by a simple space), and creates a R variable ${code{"myVector"}}, which is a vector containing the values of the file ${i{"fileForR.txt"}}.
We then apply the function ${code{"f"}} to that vector.

$br

The ${i{"fileForR.txt"}} file is set as an input file of the ${code{"RTask"}} following the syntax: ${code{"inputFiles += (om-fileVariable, \"filename-in-R-code\")"}}.
For more information about file management in OpenMOLE, see ${aa("this page", href := DocumentationPages.fileManagement.file + "#ExternalTasks")}.
$br
The end of the workflow simply tells OpenMOLE to chain the two tasks and to display the outputs of the last task (here the OpenMOLE variable ${code{"resR"}}) in the standard output.



${h2{"Using libraries"}}

Here we give an example of how to use a library in a ${code{"RTask"}}.
We use the function ${code{"CHullArea"}} of the library ${code{"GeoRange"}} to compute the area in the convex envelop of a set of points.

$br$br

We need to write the names of the libraries we need in the field ${code{"libraries"}}, as a sequence, and they will be installed from the CRAN repository.

$br

The ${code{"RTask"}} is based on a Debian container, therefore you can use any Debian command here including ${code{"apt"}} installation tool.
See advanced usage below for examples of custom commands in specific use cases.

$br$br

Note: the first time you use R with ${code{"libraries"}} or ${code{"packages"}}, it takes some time to install them, but for the next uses those libraries will be stored, and the execution will be quicker.

$br$br

${hl.openmole( s"""
    // Declare variable
    val area = Val[Double]

    // Task
    val rTask6 = RTask($tq
        library(GeoRange)

        n <- 40
        x <- rexp(n, 5)
        y <- rexp(n, 5)

        # To have the convex envelop of the set of points we created
        liste <- chull(x, y)
        hull <- cbind(x, y) [liste,]

        # require GeoRange
        area <- CHullArea(hull[, 1], hull[, 2])
        $tq,
        libraries = Seq("GeoRange")
    ) set(
        outputs += area.mapped
    )

    // Workflow
    rTask6 hook display
""")}



${h2{"Advanced RTask usage"}}

${h3{"Use a library within Docker"}}

If you are starting OpenMOLE within docker, installing ${code{"R"}} packages in a ${code{"RTask"}} might require a specific parameter setting.
The ${code{"install"}} field must be used with particular commands: we prefix install commands with ${code{"sudo"}} to get the permissions to use the Debian command ${code{"apt"}} for installation.

$br$br

${hl.openmole( s"""
    // Declare variable
    val area = Val[Double]

    // Task
    val rTask7 = RTask($tq
        library(GeoRange)

        n <- 40
        x <- rexp(n, 5)
        y <- rexp(n, 5)

        # To have the convex envelop of the set of points we created
        liste <- chull(x, y)
        hull <- cbind(x, y) [liste,]

        # require GeoRange
        area <- CHullArea(hull[, 1], hull[, 2])
        $tq,
        install = Seq("sudo apt-get update", "sudo apt-get install -y libgdal-dev libproj-dev"),
        libraries = Seq("GeoRange")
    ) set(
        outputs += area.mapped
    )

    // Workflow
    rTask7 hook display
""")}


${h3{"Use of HTTP proxy"}}

If you start OpenMOLE behind a HTTP proxy, you are probably familiar already with the ${code{"--proxy"}} parameter you can add to the OpenMOLE command line, which makes OpenMOLE use your proxy when downloading anything from the web.
You can use it like ${code{"openmole --proxy http://myproxy:3128"}}.
This proxy will also be used by OpenMOLE to download any container, including the containers used behind the curtain to run a ${code{"RTask"}}.
This proxy will also be used by the ${code{"RTask"}} to download packages from the web.

${h3{"Use alternative Debian repositories"}}

We showed how using the ${code{"install"}} parameter of a ${code{"RTask"}} enables to use Debian installation tools such as ${code{"apt"}} to install packages in the container running R.
This downloads Debian packages from the default international repositories (servers) for Debian.
In some cases, you might be willing to use alternative repositories.

$br$br

A first reason might be sleep: download and installation of packages might require hundreds of megabytes of download, leading to an important consumption of data and a slower construction of the container (only at the first execution, as the container is reused for further executions).
If your institution is running a local Debian repository, you would save data and time by using this repository.
You might also need packages which are not part of the default Debian repositories.

$br$br

You can do so by making a smart use of the ${code{"install"}} parameter to define your own repositories as shown in the example below.

$br$br

${hl.openmole( s"""
    // Declare variable
    val area = Val[Double]

    // Task
    val rTask8 = RTask($tq
        library(ggplot2)
        library(gganimate)

        # your R script here
        # [...]
        $tq,
        install = Seq(
           // replace the initial Debian repositories by my repository
           "fakeroot sed -i 's/deb.debian.org/linux.myinstitute.org/g' /etc/apt/sources.list",
           // display the list on the console so I can double check what happens
           "fakeroot cat /etc/apt/sources.list",
           // update the list of available packages (here I disable HTTP proxy as this repository is in my network)
           "fakeroot apt-get -o Acquire::http::proxy=false update ",
           // install required R packages in their binary version (quicker, much stable!)
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y r-cran-ggplot2",
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y r-cran-gganimate",
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y r-cran-plotly",
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y r-cran-ggally",
           // install the libs required for the compilation of R packages
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y libssl-dev libcurl4-openssl-dev libudunits2-dev",
           // install ffmpeg to render videos
           "DEBIAN_FRONTEND=noninteractive fakeroot apt-get -o Acquire::http::proxy=false install -y ffmpeg"
           ), //
        libraries = Seq("ggplot2", "gganimate", "plotly", "GGally")
    ) set(
        outputs += area.mapped
    )

    // Workflow
    rTask8 hook display
""")}

""")


