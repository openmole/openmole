package org.openmole.site.content.download

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

object BuildSources extends PageContent(html"""
${h2{"First time setup"}}

${h3{"Prerequisites"}}

You will need the following tools to get a local copy of OpenMOLE running:
${ul(
    li(html"A Java 11 (or higher) ${b{"JDK"}} (N.B. not only the JRE!). Check your version by typing ${code{"javac -version"}} in a terminal."),
    li(html"The ${aa("git", href := shared.link.git)} software and the ${aa("LFS", href := shared.link.gitlfs)} extension."),
    li(html"${aa("SBT", href := shared.link.sbt)}, the Scala Building Tool."),
    li(html"${aa("npm", href := shared.link.npm)}, the Node Package Manager.")
)}

As a suggestion, we recommend the ${aa("IntelliJ IDE", href := shared.link.intelliJ)} to edit the Scala source code of the project.

${h3{"Get the project"}}

Clone the OpenMOLE repository by typing the following in your command shell (prompt $$>):

${hl("""
$> git lfs install
$> git lfs clone git@github.com:openmole/openmole.git
""", "plain")
}

${h2{"Build the OpenMOLE application"}}

${h3{"Build from sources"}}

${h4{"For the first time"}}

To build the OpenMOLE application for the first time after cloning it, execute the ${code{"build.sh"}} script inside the OpenMOLE directory that you just cloned.

${hl("""
$> cd openmole
$> ./build.sh
""", "plain")
}

Upon successful completion, the executable is placed under ${i{"openmole/bin/openmole/target/assemble"}} and is launched as any executable via the ${code{"./openmole"}} command.
The app should then pop up in your default web browser, the URL should be something like ${code{"http://localhost:44961/app"}}.

${h4{"Re-build the project after an update"}}

In order to apply the changes after you updated your OpenMOLE version (by doing a ${code{"git pull"}} of the project for instance), you need to run successively the ${code{"clean.sh"}} and ${code{"build.sh"}} scripts.

${hl("""
$> ./clean.sh
$> ./build.sh
""", "plain")}

${h3{"Create a standalone archive"}}

You can create a standalone archive of your fresh OpenMOLE build and ship it around by using ${code{"sbt openmole:tar"}}.
You will find the resulting archive in ${i{"bin/openmole/target/openmole.tar.gz"}}.

$br

Publish the bundles:

${hl("""
$> cd build-system
$> sbt publish
$> cd ../libraries
$> sbt publish
$> cd ../openmole
$> sbt publish""", "plain")}

${h3{"Compile within Docker"}}

An easy way to get an OpenMOLE compilation environment up and running is to use Docker.
Once Docker is installed on your machine you can do:

${hl(s"""
$$> git clone ${shared.link.repo.openMOLEDockerBuild}
$$> cd docker-build
$$> ./run -v /a/local/path/on/your/system
# You should be in the docker container now, execute
clone
compile
""", "plain")}

You can now find the compiled OpenMOLE app in ${i{"/a/local/path/on/your/system/openmole/openmole/bin/openmole/target/assemble/"}}.

$br

Alternatively you can run the script ${i{"dev.sh"}} in the OpenMOLE git repository. It launches a docker environment, you can then run ${i{"build.sh"}} within this environment.
""")
