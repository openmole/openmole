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

object ReleaseNotesValue:
  lazy val closedIssues = Map(
    "12" -> "https://github.com/openmole/openmole/milestone/10",
    "10" -> "https://github.com/openmole/openmole/milestone/8",
    "9" -> "https://github.com/openmole/openmole/milestone/7?closed=1",
    "8" -> "https://github.com/openmole/openmole/milestone/4?closed=1",
    "7" → "https://github.com/openmole/openmole/milestone/3?closed=1",
    "6.2" → "",
    "6.1" → "https://github.com/openmole/openmole/milestone/6?closed=1",
    "6.0" → "https://github.com/openmole/openmole/milestone/2?closed=1"
  )

import ReleaseNotesValue.*

object ReleaseNotes extends PageContent(html"""

${h2{"V13 | Time Traveller"}}

${h2{"V12 | Shooting Star"}}

${h6{aa("Bug fixes", href := closedIssues("12"))}}

${h2{"V11 | R... R..."}}

${h2{"V10 | Quadratic Quest"}}

${h6{aa("Bug fixes", href := closedIssues("10"))}}

${h2{"V9 | Prolific Parrot"}}

${h6{aa("Bug fixes", href := closedIssues("9"))}}

${h2{"V8 | Origami Orchid"}}

${h3{"8.0"}}

${h6{"Core"}}

${ul(
  li{"Migrate to build to SBT 1.0"},
  li{"Migrate to Scala 2.12: faster script compilation, more concise and more efficient byte code for OpenMOLE"},
  li{"More reliable and efficient distributed execution through a complete refactor of the execution environments and migration to gridscale 2"},
  li{"Use binary serialization (boopickle) to communicate between the web app and the server: more efficient in term of CPU and bandwidth)"})}

${h6{"GUI"}}

${ul(
  li{"Move http request serializations from upickle to boopickle"},
  li{"Edition tab order can now be rearranged"},
  li{"OM specific syntax highlighting with simple completion for OM keywords and previously defined variables"},
  li{"CSV files can now be viewed as tables (with column filtering) and as plots (scatter and lines)"},
  li{"Panels layouts rearranged"},
  li{"Revamp file settings"},
  li{"Add a toOMS option on potential model files to launch the model import wizard from the file tree"},
  li{"Multiple selection for plugin removal"},
  li{"Revamp environment error from execution panel with a more reactive table"})}

${h6{"Plugins"}}

${ul(
  li{"Simplification of syntax for external tasks (mapping of variables)"},
  li{"Tasks providing containers similar to docker (udocker)"},
  li{"New language handled: RTask"},
  li{"New language handled: ScilabTask"},
  li{"New external plugin: CormasTask"},
  li{"NetLogoTask: NetLogo 6 is now supported, handling of arrays as input variables, conversion between scala and netlogo types clarified, pooling on execution nodes"},
  li{"New sampling methods: Saltelli, Morris"},
  li{"New MGO methods: NichedNSGA2, OSE"})}

${h6{"Website"}}

${ul(
  li{"Set the website responsive"},
  li{"Revamp the page organization, the navigation, assign a parent menu for almost all pages for an even better navigation"},
  li{"Fix documentation lacks about file management, scala functions, developer tools, community, etc."},
  li{"Add suggest edits button, which targets the scalatex github page in edition mode, ready for a Pull Request"},
  li{"Generate a \"Contents\" section for all pages"},
  li{"Upgrade the search efficiency"})}

${h6{aa("Bug fixes", href := closedIssues("8"))}}

${h2{"V7 | Nano Ninja"}}

${h3{"7.0"}}

${ul(
  li{"Simplification of the workflow construction: every method scheme has now its builder"},
  li{"Revamp the MGO library"},
  li{"Heavy code refactoring"},
  li{"First refactoring towards OpenMOLE as a library"},
  li{"Better resource cleaning after an execution"},
  li{"Refactoring of the build-system"},
  li{"Full integration of bootstrap.native lib through the scaladget DSL, meaning that both bootstrap and jquery dependencies have been wiped out"},
  li{"New accessibility for starting a new project thanks to the New project option button. It gathers creating a new oms file, importing a code file or downloading a market entry."},
  li{"GUI Plugins are back! All authentications in the web interface are now plugins. An example of what is looks like can be found here. Model wizards will follow. Anyway, it is now possible to build graphical extensions!"},
  li{"A top banner gives information about failed, succeeded runs or about any trouble in reaching the server."},
  li{"Support zip archives"},
  li{"Support Desktop grid environments"},
  li{"Support both care tar.gz.bin and tgz.bin archives in the model wizard section"},
  li{"Display JVM processor and memory info"},
  li{"Revamp the website"})}

${h3{"7.1"}}

${ul(
  li{"Fix bug with NetLogo precompiled code"})}

${h6{aa("Bug fixes", href := closedIssues("7"))}}

${h2{"V6 | Mostly Magic"}}

${h3{"6.0"}}

Long time no release! More than a year!
The OpenMOLE team is very proud to disclose "Mostly Magic", the 6th major version of OpenMOLE.

$br

As a reminder OpenMOLE is a software for parameter exploration, optimization, data processing which allows a simple access to large scale distributed execution environments.
It has been designed to be useful for a wide range of users and is usable even for people with no knowledge in distributed computing.

${h6{"What's new?"}}

${ul(
  li{"A new task has been introduced, the CARETask. It makes it easier than ever to run your applications and their dependencies (C, Python, R, ...) in OpenMOLE. The access to EGI is now based on DIRAC and webdav. It is more reliable, more scalable and more efficient."},
  li{"A Path type is now available in samplings to avoid copying files over local networks (use with Clusters and shared file systems only, not on EGI)."},
  li{"Authentications are now tested when they are created. Valid authentications methods are marked with a green OK tag and can be used in the workflow."},
  li{"The optimisation methods have been redesigned to be easier to use."},
  li{"A new import wizard is here to ease the integration of your programs in OpenMOLE."},
  li{"The file browser of the GUI has been revamped and is now fast and handy."},
  li{"The execution panel has been widely improved."},
  li{"A new democratic (or is it?) process to decide on the release name :)"})}

${h6{"What's to come in next release?"}}

First we plan to avoid as much as possible the long release cycles such as this one, we will make our best to shorten the release cycle of OpenMOLE to a few months.

${ul(
  li{"A new website is under development (thanks Paul & Etienne aka jQuery team)."},
  li{"The documentation will be empowered with a search function in the documentation (thanks Julien!!!)."},
  li{"A new ContainerTask will offer support for Docker / OCI containers."},
  li{"A modular OpenMOLE, with additional plugins enabled on demand."},
  li{"Improvement of the interface to be even more user friendly, with more tooltips and integrated help."})}

$br

A multi-user version that you can install on a server and be used by several user at the same time.
The possibility to delegate computation to cloud provider such as Amazon, Azure... and cloud middleware such as open stack.

${h6{aa("Bug fixes", href := closedIssues("6.0"))}}


${h3{"6.1"}}

${ul(
  li{"CARE required a kernel version greater than 3.2 which is not suitable for grid executions. A new version as been released and linked in the doc which depends on the Linux kernel 2.6.32."},
  li{"The CARETask was failing on some systems due to argument parsing."},
  li{"The market entries using CARE archives were failing due to a missing execution permission."},
  li{"The authentication add panel had no cancel button."},
  li{"A link to the demo site has been added on the website."})}

${h6{aa("Bug fixes", href := closedIssues("6.1"))}}


${h3{"6.2"}}

${ul(li{"Fix the deserialization of the authentications."})}

${h6{aa("Bug fixes", href := closedIssues("6.2"))}}

""")

