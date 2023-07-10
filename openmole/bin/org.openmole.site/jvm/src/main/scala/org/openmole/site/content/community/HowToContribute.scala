package org.openmole.site.content.community

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

object HowToContribute extends PageContent(html"""
This page is dedicated to explaining the organization of the project, the tools you need and the procedures to follow if you want to contribute painlessly to the OpenMOLE project (software and documentation).

${h2{"Contribution procedure"}}

When you want to contribute to the project (code or documentation) or if you've identified a bug in OpenMOLE, we recommend that you to start by submitting an @aa("issue", href := shared.link.issue), so that the community can identify the nature of the potential caveat.
After that, you can send a ${aa("pull request", href := shared.link.pullRequests)} so that your potential changes can be discussed.


${h3{"Create a pull request"}}

To create a pull request, you would have to:
${ul(
  li(html"""apply the ${aa("first time setup", href := DocumentationPages.buildSources.file)} of the OpenMOLE development environment,"""),
  li("""fork the repository of interest in GitHub: use the fork button in the original repository and fork it into your own GitHub account,"""),
  li("add your fork as a remote repository,"),
  li("create a branch for your modifications, add your commits there, and push this branch to your GitHub fork,"),
  li("from your GitHub fork, create a pull request of your forked repository and branch into the dev branch of the OpenMOLE repository.")
)}

Everything about pull requests is explained on the ${aa("help pages", href := shared.link.howToPR)} of GitHub.

${h2{"Build the website"}}

The source code of the website is hosted along the documentation in the OpenMOLE repository.
Here's how to build a local copy of the website/documentation.

${hl("""
$> cd openmole/openmole
$> sbt
""", "plain")}

Once sbt is launched, use the ${code{"buildSite"}} command inside sbt to build the webpages.
Location of the generated pages can be set via the ${code{"--target"}} option, e.g.

${hl("""
buildSite --target /tmp/
""","plain")}


${h3{"Edit the website or the documentation"}}

You might spot a typo/grammar mistake/bad wording, or simply want to improve a part of the documentation you think is unclear.
If so, you're more than welcomed to correct our mistakes and improve the documentation!

$br

First, make your changes locally on your clone of the OpenMOLE repo, then check your changes by building the website as explained above.
If everything works as it should and you're happy with your changes, send us a ${aa("Pull Request", href := shared.link.pullRequests)} on GitHub (all about pull requests ${aa("here", href := shared.link.howToPR)}.

$br

By default, ${b{"website pages"}} are located in ${i{"openmole/openmole/bin/org.openmole.site/jvm/target/site/"}}.
Sources for the @b{documentation pages} are located in ${i{"openmole/openmole/bin/org.openmole.site/jvm/src/main/scalatex/openmole"}}.
They are written using ${aa("scalatex", href:= shared.link.scalatex)}, a DSL to generate html content.

${h2{"Organization of the OpenMOLE project"}}

${h3{"Repositories"}}

OpenMOLE is made of three different projects:

${ul(
  li(html"${code{"openmole/openmole"}} the main project containing the source code for the core and plugins"),
  li(html"${code{"openmole/libraries"}} the libraries which OpenMOLE depends on but are not available as OSGi bundles from their developers. This project takes all these dependencies, wraps them in OSGi projects and generates the corresponding bundles to be later imported by OpenMOLE's main project."),
  li(html"${code{"openmole/build-system"}} obviously, that's the build system :) It's very unlikely that you'll have to modify this project.")
)}

${h3{"Branching model"}}

${aa("OpenMOLE repos", href := shared.link.repo.openmole)} are divided into three main branches:
${ul(
  li(html"${code{"dev"}} contains the unstable, next version of the platform. It's our development branch. Disrupting features are developed in branches, branching off @code{dev}, and are merged back into ${code{"dev"}} as soon as they are working satisfyingly enough to be operational in the next release."),
  li(html"${code{"#version-dev"}} (e.g. 15-dev) is the stable, current version of the platform. It's a maintenance branch created to be able to patch the latest-released version. Hotfixes should be developed by branching off the corresponding version-dev branch and merged back into their upstream branch and ${code{"master"}} once working correctly."),
  li(html"${code{"master"}} is the main branch, from which the two former branches are derived.")
)}


The development version of the OpenMOLE website and software is compiled and distributed several times an hour at ${aa(shared.link.next, href := shared.link.next)}.

$br

OpenMOLE applies a branching model derived from ${aa("Vincent Driessen's", href := shared.link.branchingModel)}.

The advantage of this model is that new features are tested early in interaction with each other.
This scheme serves an hybrid, date-based/feature-based release schedule.
At the beginning of a development cycle, an approximate date is given for the next release.
This date depends on the new features planned for this milestone, and it is flexible and can be modulated given the progress of the new developments.

${h2{"Tips and tricks"}}

${h3{"Error: You are not allowed to push code to this project"}}

First ensure you respect the ${a("contribution procedure", href := anchor("Contribution procedure"))} and the ${a("branching model", href := anchor("Branching model"))}.
You should not try to push directly to the OpenMOLE repository, but to your GitHub fork of the repository instead.

$br

You can identify more precisely the git error by activating debug in the git command:

${hl("""$> GIT_TRACE=1 GIT_CURL_VERBOSE=1 git push <remote_repo> <branch_name>""", "plain")}


${h3{"Error: git-lfs [...] You are not allowed to push code to this project"}}

When pushing your updates to the fork, you might encounter this error:

${hl("""
$> GIT_TRACE=1 GIT_CURL_VERBOSE=1 git push <remote_repo> <branch_name>
13:50:17.630529 trace git-lfs: run_command: ssh -p 20022 -- git@gitlab.openmole.org git-lfs-authenticate openmole/openmole.git upload
13:50:23.191218 trace git-lfs: ssh: git@gitlab.openmole.org failed, error: exit status 1, message: GitLab: You are not allowed to push code to this project.
batch request: GitLab: You are not allowed to push code to this project.: exit status 1
error: impossible de pousser des références vers 'https://github.com/<your_username>/openmole.git'
""", "plain")}

In this case, the push is failing when git lfs attempts to update the binaries referenced in the git repository into the ISCPIF gitlab.
If you did not change the binaries, you might just skip that step by using the following on the command line:

${hl("""$> git push --no-verify <remote_repo> <branch_name>""", "plain")}


${h3{"Error: OpenMOLE compiles but fails at runtime because of a package not found"}}

Try a clean and complete rebuild:

${hl("""
$> ./clean.sh
[...]
$> ./build.sh""", "plain")}


${h3{"Support for developers"}}

For developers support, remember you can get in touch with the community via:
${ul(
  li{html"""the ${a("chat", href := shared.link.chat)} in channel \"dev\""""},
  li{html"""the ${a("OpenMOLE user forum", href := shared.link.forum)}"""}
)}
""")
