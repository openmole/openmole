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

import org.openmole.site.content.header.*

object Download extends PageContent(html"""
${h2("Try our demo website")}

Before downloading OpenMOLE, you might want to get a preview of what you can do with it.
You can try OpenMOLE online using our ${aa("demo website", href := shared.link.demo)}.
Please note that the service is reset every 6 hours, so don't be surprised if your current script suddenly vanishes :-)

${h2("Install OpenMOLE")}

${h3("Prerequisites")}

${
  ul(
    li(html"${i("A Linux System")}: OpenMOLE runs on Linux, in order to run it on other systems please consider using the ${a("docker package", href := "#RuninDocker")}"),
    li(html"${i("Java >= 11")}: to run on your own computer, OpenMOLE requires Java version 11 or above. Check our ${aa("FAQ", href := DocumentationPages.faq.file)} to access your Java version information"),
    li(html"${i("Node.js")}: to run on your own computer, OpenMOLE GUI requires ${aa("Node.js", href := shared.link.nodejs)}"),
    li(html"${i("Singularity")}: most of OpenMOLE tasks require ${org.openmole.site.content.Native.singularity}")
  )
}

$br

${i("NB")}: If you have docker installed on your computer, the most convenient way to run OpenMOLE might be ${a("to run OpenMOLE in docker", href := "#RuninDocker")}.

${h3("Download")}

When Java and Node.js are installed, you just need to download and extract the archive below, and you're done!
OpenMOLE is installed and works out of the box!

$br

${linkButton(s"Download ${org.openmole.core.buildinfo.version.value} - ${org.openmole.core.buildinfo.version.name} ", Resource.script.openmole.file, classIs(btn, btn_danger), true)}

$br

The version ${i(org.openmole.core.buildinfo.version.value)}, named ${i(s"${org.openmole.core.buildinfo.version.name}")}, has been released on ${i(org.openmole.core.buildinfo.version.generationDate)}
After downloading OpenMOLE, you can launch it by executing the ${i("openmole")} file in the installation directory with the ${code("./openmole")} command.
It will bring up you web browser and you should see something like this:

${img(src := Resource.img.mole.uiScreenshot.file, `class` := "uiScreenshot", width := "100%")}

OpenMOLE supports Chrome and Firefox.
If you are using another web browser, you will need to copy paste the OpenMOLE URL (something like ${code("http://localhost:[port]")} in either Chrome or Firefox.

$br

To get help an installing OpenMOLE, get in touch with us through the OpenMOLE ${aa("chat", href := shared.link.chat)}.

${h2("Experiment with OpenMOLE")}

To get started with OpenMOLE and see a few simple use cases, you can follow our ${a("Step by Step Introduction to OpenMOLE", href := DocumentationPages.stepByStepIntro.file)}.
Other ${a("Tutorials", href := DocumentationPages.tutorials.file)} are also available, and you should find all the info you need in our ${a("Documentation", href := DocumentationPages.documentation.file)} section.

$br

If you have questions or problems, don't hesitate to contact our great community through the ${aa("forum", href := shared.link.forum)} or the ${aa("chat", href := shared.link.chat)}!

${h2{"Alternative Install"}}

${h3{"Run in Docker"}}

You can run OpenMOLE using a Docker container published ${aa("on the Docker Hub", href := org.openmole.site.shared.link.dockerHub)}.
Running OpenMOLE using Docker images facilitates the execution of multiple instances of OpenMOLE on different ports, possibly with different versions.
It also facilitates the automatic restart of OpenMOLE or its update for a newer version.

$br

You can run it using ${code{"docker"}} or ${code{"docker-compose"}}.

In one line you can run:
${hl(
  s"""#replace $$USER_DIR with a directory value in which your data will be stored
     |docker run --privileged -p 8080:8080 -h openmole -v $$USER_DIR:/var/openmole/ openmole/openmole:${org.openmole.core.buildinfo.version.value}""".stripMargin
, "plain")}

You should be able to access OpenMOLE by opening ${a(href := "http://localhost:8080", "http://localhost:8080")} in your browser.

$br

In order to use the latter, follow these steps:
${ul(
  li{html"install ${code{"docker-compose"}}: follow ${aa("the Docker documentation", href := "https://docs.docker.com/compose/install/")} to install it on your system,"},
  li{html"create a ${code{"docker-compose.yml"}} file in a directory containing the information found ${aa("on the Docker Hub", href := org.openmole.site.shared.link.dockerHub)},"},
  li{html"edit the ${code{"docker-compose.yml"}} file as explained below,"},
  li{html"pull the Docker image by running ${code{"sudo docker-compose pull"}},"},
  li{html"start the Docker image by running ${code{"sudo docker-compose up -d"}},"},
  li{html"stop the Docker image by running ${code{"sudo docker-compose down"}},"},
  li{html"monitor the Docker image by running ${code{"sudo docker-compose top"}}, ${code{"sudo docker-compose ps"}}, or ${code{"sudo docker-compose logs -t"}}."}
)}
In general, report to the @code{docker-compose} documentation and your favorite search engine to solve your problems.

$br

The following ${code{"docker-compose"}} configuration runs the ${org.openmole.core.buildinfo.version.value} OpenMOLE version.
It displays the OpenMOLE web user interface on port 55555, and mounts the local directory ${code{"./data/openmole"}} as the directory for OpenMOLE settings and results.
It also restarts automatically on failure.

${hl(s"""
version: "3"
  services:
    openmole:
      image: openmole/openmole:${org.openmole.core.buildinfo.version.value}
      hostname: openmole
      volumes:
        - ./data/openmole:/var/openmole/
      ports:
        - "55555:8080"
      privileged: true""".stripMargin, "plain")}

To set some OpenMOLE parameters (for instance an http proxy) you can do:

${hl(s"""
version: "3"
  services:
    openmole:
      image: openmole/openmole:${org.openmole.core.buildinfo.version.value}
      hostname: mymachinenetworkname.mydomain.org
      command: openmole-docker --proxy http://myproxy.mydomain.org:3128
      volumes:
        - ./data/openmole:/var/openmole/
      ports:
        - "55555:8080"
      privileged: true
      restart: on-failure:100000""".stripMargin, "plain")}


${h3{"Multi-user OpenMOLE server"}}

A multi-user OpenMOLE server host base on k3S has been developed. The instruction to deploy it are not available yet, but if you require such a sever for your lab or your company, please get in touch with us.

${h3{"Build From Sources"}}

If you prefer building the OpenMOLE application from sources you can do so as explained ${aa("here", href := DocumentationPages.buildSources.file)}.

${h3{"Get a previous version"}}

Previous versions of the OpenMOLE application and documentation are available ${aa("here", href := shared.link.allOpenMOLE)}.
The previous versions logs are gathered ${aa("here", href := DocumentationPages.releaseNotes.file)}.
"""
)
