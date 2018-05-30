
/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

import scalatags.Text.all._
import com.github.rjeschke._
import org.apache.commons.math3.genetics.GeneticAlgorithm

import scalatex.{ openmole ⇒ scalatex }
import org.openmole.tool.file._

import scalatags.Text
import scalaz.Reader

object Pages {

  /*
  def gettingStarted = Page.fromScalatex("Getting started", scalatex.GettingStarted, title = Some("Getting started with OpenMOLE - introductory tutorial with a simple workflow"))

  def stepByStepIntro = Page.fromScalatex("Step by Step Introduction", scalatex.StepByStepIntro, title = Some("A step by Step introduction for OpenMOLE newcomers "))

  def install = Page.fromScalatex("Installation Tutorial", scalatex.download.Install, title = Some("Installation tutorial "))

  def stepByStepTuto2 = Page.fromScalatex("Step By Step Model Launching Tutorial", scalatex.StepByStepTuto2, title = Some("Launching a Netlogo Model"))

  def stepByStepTuto3 = Page.fromScalatex("Step By Step Methods Tutorial", scalatex.StepByStepTuto3, title = Some("Discovering Methods  Tutorial"))

  def whoAreWe = Page("Who we are", scalatex.community.WhoAreWe(), title = Some("OpenMOLE Developers, reference publications, contact information"))

  def partner = Page("Partners", scalatex.community.Partner(), title = Some("OpenMOLE partners and collaborations"))

  val communications = Page.fromScalatex("Communications", scalatex.community.Communications, title = Some("Related scientific papers, conference slides, videos, OpenMOLE in the news"))

  def previousVersions = Page.fromScalatex("Previous versions", scalatex.download.PreviousVersions, title = Some("Previous versions of OpenMOLE"))

  val training = Page.fromScalatex("Training Sessions", scalatex.community.Training, title = Some("Live training sessions"))
*/

  // Toutes les pages à la racine du site

  // val index = Page("index", scalatex.Index(), title = Some("OpenMOLE: scientific workflow, distributed computing, parameter tuning"))
  // def faq = Page.fromScalatex("faq", scalatex.FAQ, title = Some("FAQ"))
  // val all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, stepByStepIntro, install, stepByStepTuto2, stepByStepTuto3, whoAreWe, partner, faq, communications, previousVersions, training)

  val index = Page("index", scalatex.Index(), title = Some("OpenMOLE: the Exploration Software"))

  val faq = Page.fromScalatex("FAQ", scalatex.FAQ, title = Some("Frequently Asked Questions"))

  val all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, faq)

  //def rawFile(page: Page) = page.location.mkString("_") + ".html"
  def file(page: Page) = java.net.URLEncoder.encode(page.location, "UTF-8") + ".html"

  def isDoc(page: Page) = page match {
    case d: DocumentationPage ⇒ true
    case _                    ⇒ false
  }

}

object Page {
  type ScalatexContent = { def apply(): Frag; def sourcePath: String }

  def fromScalatex[T <: Page.ScalatexContent](name: String, content: T, details: Seq[Page] = Seq(), title: Option[String] = None, extraMenu: Option[SideMenu] = None) =
    apply(name, content(), details, title, extraMenu, Some(content.sourcePath))

  def apply(name: String, content: Frag, details: Seq[Page] = Seq(), title: Option[String] = None, extraMenu: Option[SideMenu] = None, source: Option[String] = None) = {
    val (_name, _content, _details, _title, _extraMenu, _source) = (name, content, details, title, extraMenu, source)

    new Page {
      override def name: String = _name
      override def content = _content
      override def title = _title
      override def details = _details
      override def source = _source
    }
  }
}

trait Page {
  def content: Frag
  def name: String

  def title: Option[String] = Some(s"OpenMOLE - $name")
  def location: String = name
  def file = Pages.file(this)
  def details: Seq[Page]
  def source: Option[String]

  def anchor(name: String) = s"$file#${name.replaceAll(" ", "")}"
}

case class Parent[T](parent: Option[T])

object DocumentationPage {

  def fromScalatex[T <: Page.ScalatexContent](
    name:     String,
    content:  T,
    details:  ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String]           = None,
    title:    Option[String]           = None) = apply(name, content(), details, location, title, source = Some(content.sourcePath))

  def apply(
    name:     String,
    content:  ⇒ Frag,
    details:  ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String]           = None,
    title:    Option[String]           = None,
    source:   Option[String]           = None
  ) = {
    def _name = name
    def _content = content
    def _details = details
    def _location = location
    def _title = title
    def _source = source

    new DocumentationPage {
      def name = _name
      def content = _content
      override def details = _details
      override def location = _location.getOrElse(name)
      override def title = _title.orElse(Some(name))
      override def source = _source
    }
  }
}

abstract class DocumentationPage extends Page {

  override def equals(o: scala.Any): Boolean =
    o match {
      case p2: DocumentationPage ⇒ this.location == p2.location
      case _                     ⇒ false
    }

  override def hashCode(): Int = location.hashCode()
}

object DocumentationPages {

  // Toutes les pages qu'on veut dans la SiteMap

  index ⇒

  //var marketEntries: Seq[GeneratedMarketEntry] = Seq()

  def allPages = Vector[DocumentationPage](siteMap) ++ docPages ++ tutoPages ++ communityPages ++ downloadPages

  // Definir un groupe de pages
  // def groupPages = Seq(...)
  // Definir chaque page du groupe
  // lazy val pageName = DocumentationPage.fromScalatex(name = "html title", content = scalatex.path.to.scalatexFile, title = Some("Titre onglet"))

  lazy val siteMap = DocumentationPage.fromScalatex(name = "Site Map", content = scalatex.SiteMap)

  // Documentation
  def docPages =
    languagePages ++
      runPages ++
      explorePages ++
      scalePages ++
      advancedPages ++ Seq(gui, market, commandOptions)

  lazy val gui = DocumentationPage.fromScalatex(name = "GUI", content = scalatex.documentation.GUI)
  lazy val market = DocumentationPage.fromScalatex(name = "Market", content = scalatex.documentation.Market)
  lazy val commandOptions = DocumentationPage.fromScalatex(name = "Command Options", content = scalatex.documentation.CommandOptions)

  // Language
  def languagePages = developersPages ++ Seq(fileManagement, hook, language, scalaFunction, source, transition)

  lazy val fileManagement = DocumentationPage.fromScalatex(name = "File Management", content = scalatex.documentation.language.FileManagement)
  lazy val hook = DocumentationPage.fromScalatex(name = "Hook", content = scalatex.documentation.language.Hook)
  lazy val language = DocumentationPage.fromScalatex(name = "Language", content = scalatex.documentation.language.Language)
  lazy val scalaFunction = DocumentationPage.fromScalatex(name = "Scala Function", content = scalatex.documentation.language.ScalaFunction)
  lazy val source = DocumentationPage.fromScalatex(name = "Source", content = scalatex.documentation.language.Source)
  lazy val transition = DocumentationPage.fromScalatex(name = "Transition", content = scalatex.documentation.language.Transition)

  // Developers
  def developersPages = Seq(capsule, developers, console, moleTask, pluginDevelopment, serverRESTAPI, webServer)

  lazy val capsule = DocumentationPage.fromScalatex(name = "Capsule", content = scalatex.documentation.language.developers.Capsule)
  lazy val developers = DocumentationPage.fromScalatex(name = "Developers", content = scalatex.documentation.language.developers.Developers)
  lazy val console = DocumentationPage.fromScalatex(name = "Console", content = scalatex.documentation.language.developers.Console)
  lazy val moleTask = DocumentationPage.fromScalatex(name = "Mole Task", content = scalatex.documentation.language.developers.MoleTask)
  lazy val pluginDevelopment = DocumentationPage.fromScalatex(name = "Plugin Development", content = scalatex.documentation.language.developers.PluginDevelopment)
  lazy val serverRESTAPI = DocumentationPage.fromScalatex(name = "Server RESTAPI", content = scalatex.documentation.language.developers.ServerRESTAPI)
  lazy val webServer = DocumentationPage.fromScalatex(name = "WebServer", content = scalatex.documentation.language.developers.WebServer)

  // Run
  def runPages = Seq(care, container, java, netLogo, packagedCCplusplus, packagedPython, r, run, scala)

  lazy val care = DocumentationPage.fromScalatex(name = "CARE", content = scalatex.documentation.run.CARE)
  lazy val container = DocumentationPage.fromScalatex(name = "Container", content = scalatex.documentation.run.Container)
  lazy val java = DocumentationPage.fromScalatex(name = "Java", content = scalatex.documentation.run.Java)
  lazy val netLogo = DocumentationPage.fromScalatex(name = "NetLogo", content = scalatex.documentation.run.NetLogo)
  lazy val packagedCCplusplus = DocumentationPage.fromScalatex(name = "Packaged C++", content = scalatex.documentation.run.PackagedCCplusplus)
  lazy val packagedPython = DocumentationPage.fromScalatex(name = "Packaged Python", content = scalatex.documentation.run.PackagedPython)
  lazy val r = DocumentationPage.fromScalatex(name = "R", content = scalatex.documentation.run.R)
  lazy val run = DocumentationPage.fromScalatex(name = "Run", content = scalatex.documentation.run.Run)
  lazy val scala = DocumentationPage.fromScalatex(name = "Scala", content = scalatex.documentation.run.Scala)

  // Explore
  def explorePages = Seq(calibration, dataProcessing, directSampling, explore, profile, pse)

  lazy val calibration = DocumentationPage.fromScalatex(name = "Calibration", content = scalatex.documentation.explore.Calibration)
  lazy val dataProcessing = DocumentationPage.fromScalatex(name = "Data Processing", content = scalatex.documentation.explore.DataProcessing)
  lazy val directSampling = DocumentationPage.fromScalatex(name = "Direct Sampling", content = scalatex.documentation.explore.DirectSampling)
  lazy val explore = DocumentationPage.fromScalatex(name = "Explore", content = scalatex.documentation.explore.Explore)
  lazy val profile = DocumentationPage.fromScalatex(name = "Profile", content = scalatex.documentation.explore.Profile)
  lazy val pse = DocumentationPage.fromScalatex(name = "PSE", content = scalatex.documentation.explore.PSE)

  // Scale
  def scalePages = Seq(cluster, egi, island, multithread, scale, ssh)

  lazy val cluster = DocumentationPage.fromScalatex(name = "Cluster", content = scalatex.documentation.scale.Cluster)
  lazy val egi = DocumentationPage.fromScalatex(name = "EGI", content = scalatex.documentation.scale.EGI)
  lazy val island = DocumentationPage.fromScalatex(name = "Island", content = scalatex.documentation.scale.Island)
  lazy val multithread = DocumentationPage.fromScalatex(name = "Multithread", content = scalatex.documentation.scale.Multithread)
  lazy val scale = DocumentationPage.fromScalatex(name = "Scale", content = scalatex.documentation.scale.Scale)
  lazy val ssh = DocumentationPage.fromScalatex(name = "SSH", content = scalatex.documentation.scale.SSH)

  // Advanced Concepts
  def advancedPages = gaPages :+ resumableWorkflow

  lazy val resumableWorkflow = DocumentationPage.fromScalatex(name = "Resumable Workflow", content = scalatex.documentation.advancedConcepts.ResumableWorkflow)

  // Genetic Algorithms
  def gaPages = Seq(geneticAlgorithm, stochasticityManagement)

  lazy val geneticAlgorithm = DocumentationPage.fromScalatex(name = "Genetic Algorithm", content = scalatex.documentation.advancedConcepts.GA.GeneticAlgorithm)
  lazy val stochasticityManagement = DocumentationPage.fromScalatex(name = "Stochasticity Management", content = scalatex.documentation.advancedConcepts.GA.StochasticityManagement)

  // Tutorials
  def tutoPages =
    gettingStartedPages ++
      netLogoPages :+ tutorial

  lazy val tutorial = DocumentationPage.fromScalatex(name = "Tutorial", content = scalatex.tutorials.Tutorial)

  // Getting Started
  def gettingStartedPages = Seq(stepByStepIntro, launchTuto, exploreTuto)

  lazy val stepByStepIntro = DocumentationPage.fromScalatex(name = "Step By Step Intro", content = scalatex.tutorials.gettingStarted.StepByStepIntro)
  lazy val launchTuto = DocumentationPage.fromScalatex(name = "Launch Tuto", content = scalatex.tutorials.gettingStarted.LaunchTuto)
  lazy val exploreTuto = DocumentationPage.fromScalatex(name = "Explore Tuto", content = scalatex.tutorials.gettingStarted.ExploreTuto)

  // NetLogo
  def netLogoPages = Seq(netLogoGA, headlessNetLogo, simpleSAFire)

  lazy val netLogoGA = DocumentationPage.fromScalatex(name = "NetLogo GA", content = scalatex.tutorials.netLogo.NetLogoGA)
  lazy val headlessNetLogo = DocumentationPage.fromScalatex(name = "Headless NetLogo", content = scalatex.tutorials.netLogo.HeadlessNetLogo)
  lazy val simpleSAFire = DocumentationPage.fromScalatex(name = "Simple SA Fire", content = scalatex.tutorials.netLogo.SimpleSAFire)

  // Community
  def communityPages = Seq(communications, howToContribute, partner, training, whoWeAre)

  lazy val communications = DocumentationPage.fromScalatex(name = "Communications", content = scalatex.community.Communications)
  lazy val howToContribute = DocumentationPage.fromScalatex(name = "How To Contribute", content = scalatex.community.HowToContribute)
  lazy val partner = DocumentationPage.fromScalatex(name = "Partner", content = scalatex.community.Partner)
  lazy val training = DocumentationPage.fromScalatex(name = "Training", content = scalatex.community.Training)
  lazy val whoWeAre = DocumentationPage.fromScalatex(name = "Who We Are", content = scalatex.community.WhoWeAre)

  // Download
  def downloadPages = Seq(install, previousVersions)

  lazy val install = DocumentationPage.fromScalatex(name = "Install Tuto", content = scalatex.download.Install)
  lazy val previousVersions = DocumentationPage.fromScalatex(name = "Previous Versions", content = scalatex.download.PreviousVersions)

  /*
  def allPages = Vector[DocumentationPage](
    docSiteMap,
    gui,
    model,
    language,
    howToContribute,
    environment,
    multithread,
    ssh,
    egi,
    cluster,
    method,
    calibration,
    profile,
    pse,
    tutorial,
    resume,
    headlessNetLogo,
    netLogoGA,
    simpleSAFire,
    capsule,
    scalaFunction,
    plugin,
    webserver,
    dataProcessing,
    advancedConcepts,
    directSampling,
    transition,
    hook,
    source,
    console
  ) ++ modelPages ++ extraModelPages ++ detailsPages ++ advancedPages

  lazy val topPages = Seq(
    modelPages,
    methodPages,
    environmentPages
  ).flatten ++ Seq(model, method, environment)

  lazy val gui = DocumentationPage.fromScalatex(name = "GUI guide", content = scalatex.documentation.GUI)

  def modelPages = Seq(scala, java, r, netLogo, container, care, mole)
  def extraModelPages = Seq(packagedR, packagedPython, packagedCPlusPlus)

  lazy val scala = DocumentationPage.fromScalatex(name = "Scala", content = scalatex.documentation.language.model.Scala, details = Seq(scalaFunction))
  lazy val java = DocumentationPage.fromScalatex(name = "Java", content = scalatex.documentation.language.model.Java)

  lazy val r = DocumentationPage.fromScalatex(name = "R", content = scalatex.documentation.language.model.R)
  lazy val container = DocumentationPage.fromScalatex(name = "Container", content = scalatex.documentation.language.model.Container, title = Some("Native Code In a Container"))
  lazy val care = DocumentationPage.fromScalatex(name = "CARE", content = scalatex.documentation.language.model.CARE, title = Some("Native Code Packaging, CARE Task"))

  lazy val packagedCPlusPlus = DocumentationPage.fromScalatex(name = "Packaged C++", location = Some("cplusplus"), content = scalatex.documentation.language.model.PackagedCCplusplus, details = Seq(care))
  lazy val packagedR = DocumentationPage.fromScalatex(name = "Packaged R", content = scalatex.documentation.language.model.PackagedR, details = Seq(care))
  lazy val packagedPython = DocumentationPage.fromScalatex(name = "Packaged Python", content = scalatex.documentation.language.model.PackagedPython, details = Seq(care))

  lazy val netLogo = DocumentationPage.fromScalatex(name = "NetLogo", content = scalatex.documentation.language.model.NetLogo)
  lazy val mole = DocumentationPage.fromScalatex(name = "Mole Task", content = scalatex.documentation.language.model.MoleTask)
  lazy val model = DocumentationPage.fromScalatex(name = "Models", content = scalatex.documentation.language.Model)

  def languagePages = Seq(model, environment, method)

  lazy val language = DocumentationPage.fromScalatex(name = "Language", content = scalatex.documentation.Language, title = Some("OpenMOLE Domain Specific Language"))

  lazy val transition = DocumentationPage.fromScalatex(name = "Transitions", content = scalatex.documentation.language.advanced.Transition)
  lazy val hook = DocumentationPage.fromScalatex(name = "Hooks", content = scalatex.documentation.language.advanced.Hook)
  lazy val source = DocumentationPage.fromScalatex(name = "Sources", content = scalatex.documentation.language.advanced.Source)
  lazy val capsule = DocumentationPage.fromScalatex(name = "Capsule", content = scalatex.documentation.language.advanced.Capsule)
  lazy val dataflow = DocumentationPage.fromScalatex(name = "Dataflow", content = scalatex.documentation.language.advanced.Dataflow)
  lazy val scalaFunction = DocumentationPage.fromScalatex(name = "Utility Scala functions", content = scalatex.documentation.language.advanced.ScalaFunction)
  lazy val fileManagement = DocumentationPage.fromScalatex(name = "File Management", content = scalatex.documentation.language.advanced.FileManagement)

  lazy val environment = DocumentationPage.fromScalatex(name = "Environments", content = scalatex.documentation.language.Environment)

  lazy val multithread = DocumentationPage.fromScalatex(name = "Multi-threads", content = scalatex.documentation.language.environment.Multithread)
  lazy val ssh = DocumentationPage.fromScalatex(name = "SSH", content = scalatex.documentation.language.environment.SSH)
  lazy val egi = DocumentationPage.fromScalatex(name = "EGI", content = scalatex.documentation.language.environment.EGI)
  lazy val cluster = DocumentationPage.fromScalatex(
    name = "Clusters",
    content = scalatex.documentation.language.environment.Cluster
  )

  def environmentPages = Seq(multithread, ssh, egi, cluster)

  def methodPages = Seq(directSampling, calibration, profile, pse, dataProcessing)

  lazy val tutorialPages = Seq(
    ).gettingStarted,
    Pages.stepByStepIntro,
    Pages.install,
    Pages.stepByStepTuto2,
    Pages.stepByStepTuto3,
    netLogoGA,
    resume)

  lazy val detailsPages = Seq(
    geneticalgo,
    island,
    stochasticity,
    care,
    headlessNetLogo,
    gui,
    language
  )

  def advancedPages = Seq(
    transition,
    hook,
    source,
    capsule,
    dataflow,
    fileManagement
  )

  lazy val developmentPages = Seq(
    plugin,
    webserver,
    console
  )

  lazy val communityCommunicationPages = Seq(
    Pages.faq,
    Pages.partner,
    Pages.communications,
    Pages.previousVersions,
    Pages.training,
    Pages.whoAreWe
  )

  lazy val HPdownloadPages = Seq(
    Pages.install,
    Pages.previousVersions
  )

  lazy val method = DocumentationPage.fromScalatex(name = "Methods", content = scalatex.documentation.language.Method)

  lazy val calibration = DocumentationPage.fromScalatex(name = "Calibration", content = scalatex.documentation.language.method.Calibration, details = Seq(geneticalgo, island, stochasticity))
  lazy val geneticalgo = DocumentationPage.fromScalatex(name = "Genetic Algorithms", content = scalatex.documentation.details.GeneticAlgorithm)
  lazy val island = DocumentationPage.fromScalatex(name = "Islands Scheme", content = scalatex.documentation.details.Island, title = Some("Island repartition scheme"))
  lazy val stochasticity = DocumentationPage.fromScalatex(name = "Stochasticity management", content = scalatex.documentation.details.StochasticityManagement)

  lazy val profile = DocumentationPage.fromScalatex(name = "Profiles", content = scalatex.documentation.language.method.Profile)
  lazy val pse = DocumentationPage.fromScalatex(name = "PSE", content = scalatex.documentation.language.method.PSE)

  lazy val directSampling = DocumentationPage.fromScalatex(name = "Direct Sampling", content = scalatex.documentation.language.method.DirectSampling)

  lazy val dataProcessing = DocumentationPage.fromScalatex(name = "Data Processing", content = scalatex.documentation.language.method.DataProcessing)

  lazy val advancedConcepts = DocumentationPage.fromScalatex(name = "Advanced Concepts", content = scalatex.documentation.language.AdvancedConcepts)

  lazy val tutorial = DocumentationPage.fromScalatex(name = "Tutorials", content = scalatex.documentation.language.Tutorial)
  lazy val resume = DocumentationPage.fromScalatex(name = "Resume Workflow", content = scalatex.documentation.language.tutorial.Resume, title = Some("How to build a resumable workflow"))
  lazy val headlessNetLogo = DocumentationPage.fromScalatex(name = "Netlogo Headless", content = scalatex.documentation.language.advanced.HeadlessNetLogo, title = Some("Headless version of Netlog model"))

  lazy val netLogoGA = DocumentationPage.fromScalatex(name = "GA with NetLogo", content = scalatex.documentation.language.tutorial.NetLogoGA, title = Some("Calibrate a NetLogo model using genetic algorithms"))
  lazy val simpleSAFire = DocumentationPage.fromScalatex(name = "Sensitivity Analysis on NetLogo", content = scalatex.documentation.language.tutorial.simpleSAFire, title = Some("One factor Senstivity Analysis on a Netlogo model"))

  lazy val plugin = DocumentationPage.fromScalatex(name = "Plugin", content = scalatex.documentation.language.advanced.PluginDevelopment)
  lazy val webserver = DocumentationPage.fromScalatex(name = "Web Server", content = scalatex.documentation.development.WebServer, title = Some("Webserver and Rest API"))
  lazy val howToContribute = DocumentationPage.fromScalatex(name = "How to Contribute", content = scalatex.documentation.development.howToContribute)
  lazy val console = DocumentationPage.fromScalatex(name = "Console mode", content = scalatex.documentation.development.Console)
*/

  //    val market = new DocumentationPage {
  //      override def content: Text.all.Frag = div(tagContent(marketEntries))
  //
  //      override def children: Seq[DocumentationPage] = Seq()
  //
  //      override def name: String = "Market"
  //
  //      override def details: Seq[Page] = Seq()
  //
  //      def tagContent(entries: Seq[GeneratedMarketEntry]) =
  //        ul(
  //          entries.sortBy(_.entry.name.toLowerCase).map {
  //            de ⇒ li(entryContent(de))
  //          }: _*
  //        )
  //
  //      def entryContent(deployedMarketEntry: GeneratedMarketEntry) = {
  //        def title: Modifier =
  //          deployedMarketEntry.viewURL match {
  //            case None    ⇒ deployedMarketEntry.entry.name
  //            case Some(l) ⇒ a(deployedMarketEntry.entry.name, href := l)
  //          }
  //
  //        def content =
  //          Seq[Modifier](
  //            deployedMarketEntry.readme.map {
  //              rm ⇒ RawFrag(txtmark.Processor.process(rm))
  //            }.getOrElse(p("No README.md available yet.")),
  //            a("Packaged archive", href := deployedMarketEntry.archive), " (can be imported in OpenMOLE)"
  //          ) ++ deployedMarketEntry.viewURL.map(u ⇒ br(a("Source repository", href := u)))
  //
  //        div(scalatags.Text.all.id := "market-entry")(content: _*)
  //      }
  //
  //      def themes: Seq[Market.Tag] = {
  //        marketEntries.flatMap(_.entry.tags).distinct.sortBy(_.label.toLowerCase)
  //      }
  //
  //    }

}
