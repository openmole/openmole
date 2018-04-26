package org.openmole.site

/*
 * Copyright (C) 11/05/17 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import scalatags.Text.all._

package object shared {
  lazy val searchDiv = "search-div"
  lazy val searchImg = "search-img"
  lazy val blogposts = "blog-posts"
  lazy val newsPosts = "news-posts"
  lazy val shortTraining = "short-training"
  lazy val longTraining = "long-training"

  object profile {
    val button = "profileTrigger"
    val animation = "startProfileAnim"
  }

  object pse {
    val button = "pseTrigger"
    val animation = "startPseAnim"
  }

  object sensitivity {
    val button = "sensitivityTrigger"
    val animation = "startSensitivityAnim"
  }

  object faq {
    lazy val javaVersion = "Which Java version should I use?"
    lazy val oldVersions = "Where can I find the old versions of OpenMOLE?"
    lazy val sshConnectionBug = "OpenMOLE cannot connect to my environment using SSH"
    lazy val passwordAuthentication = "Password authentication"
    lazy val isOpenMOLEup = "Is OpenMOLE doing something?"
    lazy val homeQuota = "I've reached my home folder size / file quota"
    lazy val sampleError = "My sampling generates a type error"
    lazy val cannotGetCare = "I can't get CARE / PRoot to work"
    lazy val tooManyOpenFiles = "I get an error relate to file on linux and there is 'too many open files' written somewhere in the error"
    lazy val qxcbConnection = "My workflow fails with QXcbConnection: Could not connect to display"
    lazy val pathOverFile = "When shall I use Path over File?"
    lazy val notListed = "My problem is not listed here"
  }

  object guiGuide {
    lazy val overview = "Overview"
    lazy val startProject = "Starting a project"
    lazy val fileManagment = "File Management"
    lazy val authentication = "Authentications"
    lazy val playAndMonitor = "Play and monitor executions"
    lazy val plugin = "Plugins"
  }

  object nativeModel {
    lazy val rExample = "An example with R"
    lazy val pythonExample = "Another example with Python"
    lazy val advancedOptions = "Advanced options"
  }

  object howToContributeMenu {
    lazy val prerequisites = "Prerequisites"
    lazy val firstTimeSetup = "First time setup"
    lazy val buildAppFromSources = "Build from Sources"
    lazy val standaloneArchive = "Standalone archive"
    lazy val compileDocker = "Compile within Docker"
    lazy val buildWebsite = "Build the website"
    lazy val repositories = "Repositories"
    lazy val webpagesSources = "Edit Documentation pages"
    lazy val devVersion = "Dev versions "
    lazy val projectOrganization = "Project organization"
    lazy val bugReport = "Bug Report"
    lazy val contributionProcedure = "Contribution procedure"
    lazy val branchingModel = "Branching model"
    lazy val tipsAndTricks = "Tips and tricks"
  }

  object rTask {
    lazy val rVersion = "3.3.3"
  }

  object gaWithNetlogo {
    lazy val antModel = "The ant model"
    lazy val defineProblem = "An optimisation problem"
    lazy val runOpeMOLE = "Run it in OpenMOLE"
    lazy val optimizationAlgo = "The optimisation algorithm"
    lazy val scaleUp = "Scale up"
  }

  object simpleSAFireMenu {
    lazy val sensitivityAnalysis = "Sensitivity Analysis"
    lazy val sensitivityFireModel = "Real world Example"
  }

  object documentationSideMenu {
    lazy val cssClass = "sidemenu"
    lazy val place = "side-menu-place"
  }

  def anchor(title: String) = title.filter(c ⇒ c.isLetterOrDigit)

  object link {

    lazy val demo = "http://demo.openmole.org"
    lazy val twitter = "https://twitter.com/OpenMOLE"
    lazy val blog = "https://blog.openmole.org"
    lazy val chat = "https://chat.iscpif.fr/channel/openmole"
    lazy val simpluDemo = "https://simplu.openmole.org"
    lazy val mailingList = "http://ask.openmole.org"
    lazy val shortTrainings = "https://iscpif.fr/events/formationsjedi/"
    lazy val longTrainings = "http://cnrsformation.cnrs.fr"
    lazy val egi = "http://www.egi.eu/"
    lazy val batchProcessing = "https://en.wikipedia.org/wiki/Batch_processing"
    lazy val batchSystem = "http://en.wikipedia.org/wiki/Portable_Batch_System"
    lazy val grieEngine = "https://en.wikipedia.org/wiki/Oracle_Grid_Engine"
    lazy val slurm = "https://en.wikipedia.org/wiki/Simple_Linux_Utility_for_Resource_Management"
    lazy val condor = "https://en.wikipedia.org/wiki/HTCondor"
    lazy val oar = "http://oar.imag.fr/dokuwiki/doku.php"
    lazy val ssh = "https://en.wikipedia.org/wiki/Secure_Shell"
    lazy val geodivercity = "http://geodivercity.parisgeo.cnrs.fr/blog/"
    lazy val ercSpringer = "http://www.springer.com/fr/book/9783319464954"
    lazy val git = "https://git-scm.com/"
    lazy val gitlfs = "https://git-lfs.github.com/"
    lazy val sbt = "http://www.scala-sbt.org/"
    lazy val scala = "http://www.scala-lang.org/"
    lazy val scalaBook = "http://www.scala-lang.org/node/959"
    lazy val scalaDoc = "http://www.scala-lang.org/api/current/index.html"
    lazy val intelliJ = "https://www.jetbrains.com/idea/"
    lazy val scalatex = "http://www.lihaoyi.com/Scalatex/"
    lazy val netlogoAnts = "http://ccl.northwestern.edu/netlogo/models/Ants"
    lazy val branchingModel = "http://nvie.com/posts/a-successful-git-branching-model/"
    lazy val issue = "https://github.com/openmole/openmole/issues"
    lazy val pullRequests = "https://github.com/openmole/openmole/pulls"
    lazy val next = "https://next.openmole.org/"
    lazy val CAREsite = "https://proot-me.github.io/"
    lazy val CAREmailing = "https://groups.google.com/forum/?fromgroups#!forum/reproducible"
    lazy val ggplot2 = "http://ggplot2.tidyverse.org/reference/"
    lazy val sobol = "https://en.wikipedia.org/wiki/Sobol_sequence"
    lazy val lhs = "https://en.wikipedia.org/wiki/Latin_hypercube_sampling"
    lazy val jce = "http://www.oracle.com/technetwork/java/javase/downloads/index.html"
    lazy val allOpenMOLE = "http://www.openmole.org/all/"
    lazy val care = "https://github.com/proot-me/proot-static-build/releases/download/v5.1.1/care_2.2.2_x86_64_rc2--no-seccomp"
    lazy val prootIssue106 = "https://github.com/proot-me/PRoot/issues/106"
    lazy val xvfb = "https://www.x.org/releases/X11R7.7/doc/man/man1/Xvfb.1.xhtml"
    lazy val prootStatic = "https://github.com/proot-me/proot-static-build/tree/master/static"
    lazy val multiobjectiveOptimization = "http://en.wikipedia.org/wiki/Multiobjective_optimization"
    lazy val paretoEfficency = "http://en.wikipedia.org/wiki/Pareto_efficiency"
    lazy val openMOLEWiki = "https://github.com/openmole/openmole/wiki"
    lazy val noveltySearch = "http://eplex.cs.ucf.edu/noveltysearch/userspage/"
    lazy val javaString = "https://docs.oracle.com/javase/7/docs/api/java/lang/String.html"
    lazy val javaFile = "https://docs.oracle.com/javase/8/docs/api/java/io/File.html"
    lazy val npm = "https://www.npmjs.com/get-npm"

    object paper {
      lazy val jassCP = "http://jasss.soc.surrey.ac.uk/18/1/12.html"
      lazy val fgcs2013 = "http://www.sciencedirect.com/science/article/pii/S0167739X13001027"
      lazy val fgcs2013preprint = "https://hal-paris1.archives-ouvertes.fr/hal-00840744/"
      lazy val hpcs2010 = "http://ieeexplore.ieee.org/document/5547155/"
      lazy val beyondCorroboration = "http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0138212"
      lazy val halfBillionOA = "https://hal.archives-ouvertes.fr/hal-01118918"
      lazy val halfBillionEditor = "http://journals.sagepub.com/doi/abs/10.1068/b130064p"
      lazy val jass2015 = "http://jasss.soc.surrey.ac.uk/18/4/9.html"
      lazy val mdpi2015 = "http://www.mdpi.com/2079-8954/3/4/348"
      lazy val frontier2017 = "http://journal.frontiersin.org/article/10.3389/fninf.2017.00021/full#"
      lazy val urbanDynamics = "https://hal.archives-ouvertes.fr/view/index/docid/1583528"
      lazy val urbanDynamicsBib = "https://hal.archives-ouvertes.fr/hal-01583528v1/bibtex"
    }

    object partner {
      lazy val iscpif = "http://iscpif.fr"
      lazy val parisgeo = "http://www.parisgeo.cnrs.fr/"
      lazy val biomedia = "https://biomedia.doc.ic.ac.uk/"
      lazy val idf = "https://www.iledefrance.fr/"
      lazy val paris = "https://www.paris.fr/"
      lazy val ign = "http://www.ign.fr/"
    }

    object repo {
      lazy val openmole = "https://github.com/openmole/openmole"
      lazy val market = "https://github.com/openmole/openmole-market"
      lazy val gridscale = "https://github.com/openmole/gridscale"
      lazy val scaladget = "https://github.com/openmole/scaladget"
      lazy val scalawui = "https://github.com/openmole/scalaWUI"
      lazy val mgo = "https://github.com/openmole/mgo"
      lazy val simplu = "https://github.com/IGNF/simplu3D"
      lazy val myOpenmolePlugin = "https://github.com/openmole/myopenmoleplugin"
      lazy val gamaPlugin = "https://github.com/openmole/gama-plugin"
      lazy val openMOLEDockerBuild = "https://github.com/openmole/docker-build.git"
    }

  }

  def rawFrag(content: String) = {
    val builder = new scalatags.text.Builder()
    raw(content).applyTo(builder)
    div(textAlign := "center")(builder.children.head)
  }

  import link._

  val links = Seq(
    partner.iscpif,
    partner.parisgeo,
    partner.biomedia,
    partner.idf,
    partner.paris,
    partner.ign,
    paper.jassCP,
    paper.fgcs2013,
    paper.fgcs2013preprint,
    paper.hpcs2010,
    paper.beyondCorroboration,
    paper.halfBillionOA,
    paper.halfBillionEditor,
    paper.jass2015,
    paper.mdpi2015,
    paper.frontier2017,
    paper.urbanDynamics,
    paper.urbanDynamicsBib,
    repo.openmole,
    repo.market,
    repo.gridscale,
    repo.scaladget,
    repo.scalawui,
    repo.mgo,
    repo.simplu,
    repo.myOpenmolePlugin,
    repo.gamaPlugin,
    repo.openMOLEDockerBuild,
    demo,
    twitter,
    blog,
    chat,
    simpluDemo,
    mailingList,
    shortTrainings,
    longTrainings,
    egi,
    batchProcessing,
    batchSystem,
    grieEngine,
    slurm,
    condor,
    oar,
    ssh,
    geodivercity,
    ercSpringer,
    git,
    gitlfs,
    sbt,
    scala,
    scalaBook,
    scalaDoc,
    intelliJ,
    scalatex,
    netlogoAnts,
    branchingModel,
    issue,
    pullRequests,
    next,
    CAREsite,
    CAREmailing,
    ggplot2,
    sobol,
    lhs,
    jce,
    allOpenMOLE,
    care,
    prootIssue106,
    xvfb,
    prootStatic,
    multiobjectiveOptimization,
    paretoEfficency,
    openMOLEWiki,
    noveltySearch,
    javaString,
    javaFile
  )
}
