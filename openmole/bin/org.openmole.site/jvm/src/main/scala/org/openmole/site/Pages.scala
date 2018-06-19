
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

  // Toutes les pages à la racine du site
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

  def allPages = headPages ++ docPages ++ tutoPages ++ communityPages ++ downloadPages

  // Definir un groupe de pages
  // def groupPages = Seq(...)
  // Definir chaque page du groupe
  // lazy val pageName = DocumentationPage.fromScalatex(name = "html title", content = scalatex.path.to.scalatexFile, title = Some("Titre onglet"))

  def headPages =
    Seq(documentation, run, explore, scale, language, advanced, advancedConcepts, developers, tutorials, OMcommunity)

  // Documentation
  lazy val documentation = DocumentationPage.fromScalatex(name = "Documentation", content = scalatex.Documentation)

  def docPages =
    runPages ++
      explorePages ++
      scalePages ++
      languagePages ++
      advancedConceptsPages ++
      docLonelyPages

  def docLonelyPages = Seq(gui, market, commandOptions)

  lazy val gui = DocumentationPage.fromScalatex(name = "GUI", content = scalatex.documentation.GUI)
  lazy val market = DocumentationPage.fromScalatex(name = "Market", content = scalatex.documentation.Market)
  lazy val commandOptions = DocumentationPage.fromScalatex(name = "Command Options", content = scalatex.documentation.CommandOptions)

  // Language
  lazy val language = DocumentationPage.fromScalatex(name = "Language", content = scalatex.documentation.language.Language)

  def languagePages = advancedPages ++ Seq(fileManagement, hook, scalaFunction)

  lazy val fileManagement = DocumentationPage.fromScalatex(name = "File Management", content = scalatex.documentation.language.FileManagement)
  lazy val hook = DocumentationPage.fromScalatex(name = "Hook", content = scalatex.documentation.language.Hook)
  lazy val scalaFunction = DocumentationPage.fromScalatex(name = "Scala Function", content = scalatex.documentation.language.ScalaFunction)

  // Advanced
  lazy val advanced = DocumentationPage.fromScalatex(name = "Advanced", content = scalatex.documentation.language.advanced.Advanced)

  def advancedPages = Seq(capsule, moleTask, source, transition)

  lazy val capsule = DocumentationPage.fromScalatex(name = "Capsule", content = scalatex.documentation.language.advanced.Capsule)
  lazy val moleTask = DocumentationPage.fromScalatex(name = "Mole Task", content = scalatex.documentation.language.advanced.MoleTask)
  lazy val source = DocumentationPage.fromScalatex(name = "Source", content = scalatex.documentation.language.advanced.Source)
  lazy val transition = DocumentationPage.fromScalatex(name = "Transition", content = scalatex.documentation.language.advanced.Transition)

  // Run
  lazy val run = DocumentationPage.fromScalatex(name = "Run", content = scalatex.documentation.run.Run)

  def runPages = Seq(java, netLogo, r, scilab, care, packagedCCplusplus, packagedPython, container, scala)

  lazy val care = DocumentationPage.fromScalatex(name = "CARE", content = scalatex.documentation.run.CARE)
  lazy val container = DocumentationPage.fromScalatex(name = "Container", content = scalatex.documentation.run.Container)
  lazy val java = DocumentationPage.fromScalatex(name = "Java", content = scalatex.documentation.run.Java)
  lazy val netLogo = DocumentationPage.fromScalatex(name = "NetLogo", content = scalatex.documentation.run.NetLogo)
  lazy val packagedCCplusplus = DocumentationPage.fromScalatex(name = "Packaged C/C++", content = scalatex.documentation.run.PackagedCCplusplus)
  lazy val packagedPython = DocumentationPage.fromScalatex(name = "Packaged Python", content = scalatex.documentation.run.PackagedPython)
  lazy val r = DocumentationPage.fromScalatex(name = "R", content = scalatex.documentation.run.R)
  lazy val scala = DocumentationPage.fromScalatex(name = "Scala", content = scalatex.documentation.run.Scala)
  lazy val scilab = DocumentationPage.fromScalatex(name = "Scilab", content = scalatex.documentation.run.Scilab)

  // Explore
  lazy val explore = DocumentationPage.fromScalatex(name = "Explore", content = scalatex.documentation.explore.Explore)

  def explorePages = Seq(calibration, dataProcessing, directSampling, profile, pse)

  lazy val calibration = DocumentationPage.fromScalatex(name = "Calibration", content = scalatex.documentation.explore.Calibration)
  lazy val dataProcessing = DocumentationPage.fromScalatex(name = "Data Processing", content = scalatex.documentation.explore.DataProcessing)
  lazy val directSampling = DocumentationPage.fromScalatex(name = "Direct Sampling", content = scalatex.documentation.explore.DirectSampling)
  lazy val profile = DocumentationPage.fromScalatex(name = "Profile", content = scalatex.documentation.explore.Profile)
  lazy val pse = DocumentationPage.fromScalatex(name = "PSE", content = scalatex.documentation.explore.PSE)

  // Scale
  lazy val scale = DocumentationPage.fromScalatex(name = "Scale", content = scalatex.documentation.scale.Scale)

  def scalePages = Seq(cluster, egi, island, multithread, ssh)

  lazy val cluster = DocumentationPage.fromScalatex(name = "Cluster", content = scalatex.documentation.scale.Cluster)
  lazy val egi = DocumentationPage.fromScalatex(name = "EGI", content = scalatex.documentation.scale.EGI)
  lazy val island = DocumentationPage.fromScalatex(name = "Island", content = scalatex.documentation.scale.Island)
  lazy val multithread = DocumentationPage.fromScalatex(name = "Multithread", content = scalatex.documentation.scale.Multithread)
  lazy val ssh = DocumentationPage.fromScalatex(name = "SSH", content = scalatex.documentation.scale.SSH)

  // Advanced Concepts
  lazy val advancedConcepts = DocumentationPage.fromScalatex(name = "Advanced Concepts", content = scalatex.documentation.advancedConcepts.AdvancedConcepts)

  def advancedConceptsPages = gaPages :+ resumableWorkflow

  lazy val resumableWorkflow = DocumentationPage.fromScalatex(name = "Resumable Workflow", content = scalatex.documentation.advancedConcepts.ResumableWorkflow)

  // Genetic Algorithms
  def gaPages = Seq(geneticAlgorithm, stochasticityManagement)

  lazy val geneticAlgorithm = DocumentationPage.fromScalatex(name = "Genetic Algorithm", content = scalatex.documentation.advancedConcepts.GA.GeneticAlgorithm)
  lazy val stochasticityManagement = DocumentationPage.fromScalatex(name = "Stochasticity Management", content = scalatex.documentation.advancedConcepts.GA.StochasticityManagement)

  // Developers
  lazy val developers = DocumentationPage.fromScalatex(name = "Developers", content = scalatex.documentation.developers.Developers)

  def developersPages = Seq(capsule, console, moleTask, pluginDevelopment, serverRESTAPI, webServer)

  lazy val console = DocumentationPage.fromScalatex(name = "Console", content = scalatex.documentation.developers.Console)
  lazy val pluginDevelopment = DocumentationPage.fromScalatex(name = "Plugin Development", content = scalatex.documentation.developers.PluginDevelopment)
  lazy val serverRESTAPI = DocumentationPage.fromScalatex(name = "Server RESTAPI", content = scalatex.documentation.developers.ServerRESTAPI)
  lazy val webServer = DocumentationPage.fromScalatex(name = "Web Server", content = scalatex.documentation.developers.WebServer)

  // Tutorials
  lazy val tutorials = DocumentationPage.fromScalatex(name = "Tutorials", content = scalatex.tutorials.Tutorials)

  def tutoPages = gettingStartedPages ++ netLogoPages

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
  lazy val OMcommunity = DocumentationPage.fromScalatex(name = "Community", content = scalatex.community.OMCommunity)

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
