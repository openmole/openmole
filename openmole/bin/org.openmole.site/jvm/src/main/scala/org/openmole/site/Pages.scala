
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

import PageTree._
object Pages {

  // Toutes les pages à la racine du site
  val indexPage = Page("index", scalatex.Index(), title = Some("OpenMOLE: the Model Exploration Software"))
  val index = pageNode(indexPage)

  val all: Vector[PageTree] = (DocumentationPages.allPages :+ index).toVector

  //def rawFile(page: Page) = page.location.mkString("_") + ".html"
  def file(page: Page) = java.net.URLEncoder.encode(page.location, "UTF-8") + ".html"

  def isDoc(page: Page) = page match {
    case d: DocumentationPage ⇒ true
    case _                    ⇒ false
  }
}

object PageTree {
  implicit def fromPage(p: Page) = PageLeaf(p)

  implicit def fromSeqPage(ps: Seq[Page]) = ps.map {
    fromPage
  }

  implicit def fromPageTreeS(pageNode: PageTree): Seq[Page] = pageNode.sons.map(_.page)

  implicit def fromPageTree(pageTree: PageTree): Page = pageTree.page

  def pageNode(page: Page, sons: Vector[PageTree]): PageTree = {
    PageNode(page, sons)
  }

  // def pageNode(sons: Vector[PageTree]): PageNode = PageNode(None, sons)

  def pageNode(page: Page): PageTree = PageLeaf(page)

  lazy val parentsMap = {
    DocumentationPages.headPages.flatMap { hp ⇒
      hp.sons.map { s ⇒ s.name -> hp }
    }.filterNot(_._1.isEmpty).toMap
  }

  def parents(pageTree: PageTree) = {

    def parents0(pageTree: PageTree, pars: Seq[PageTree]): Seq[PageTree] = {
      parentsMap.get(pageTree.name) match {
        case Some(p: PageTree) ⇒ parents0(p, pars :+ p)
        case None              ⇒ pars
      }
    }

    parents0(pageTree, Seq())
  }
}

sealed trait PageTree {
  def page: Page
  def name: String = page.name
  def title: Option[String] = page.title
  def source = page.source
  def content: Text.all.Frag = page.content
  def hasSon(pageTree: PageTree) = sons.contains(pageTree)
  def sons: Vector[PageTree]
}

case class PageNode(page: Page, sons: Vector[PageTree]) extends PageTree

case class PageLeaf(page: Page) extends PageTree {
  val sons = Vector()
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
  def source: Option[String]
  def anchor(name: String) = s"$file#${name.replaceAll(" ", "")}"
}

object DocumentationPage {

  def fromScalatex[T <: Page.ScalatexContent](
    name:     String,
    content:  T,
    details:  ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String]           = None,
    title:    Option[String]           = None) =
    apply(name, content(), details, location, title, source = Some(content.sourcePath))

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

import PageTree._

object DocumentationPages {
  index ⇒

  def allPages = docPages.flatMap { _.sons } ++ tutoPages.sons ++ communityPages.sons ++ downloadPages.sons ++ headPages

  def headPages: Seq[PageTree] = docPages ++ Seq(runPages, explorePages, scalePages, languagePages, developersPages, tutoPages, communityPages, downloadPages)

  val mainDocPages = runPages.sons.map {
    _.page
  } ++ explorePages.sons.map {
    _.page
  } ++ scalePages.sons.map {
    _.page
  } ++ Seq(scale, explore, run)

  // Documentation
  lazy val documentation = DocumentationPage.fromScalatex(name = "Documentation", content = scalatex.documentation.Documentation)

  def docPages = Seq(
    runPages,
    packagedPages,
    explorePages,
    samplingPages,
    scalePages,
    languagePages,
    advancedConceptsPages,
    developersPages,
    docLonelyPages
  )

  def docLonelyPages = pageNode(documentation, Vector(gui, commandOptions, faq))

  lazy val gui = DocumentationPage.fromScalatex(name = "GUI", content = scalatex.documentation.GUI, title = Some("Graphical User Interface"))
  lazy val commandOptions = DocumentationPage.fromScalatex(name = "Command Options", content = scalatex.documentation.CommandOptions)
  val faq = DocumentationPage.fromScalatex(name = "FAQ", content = scalatex.FAQ, title = Some("Frequently Asked Questions"))

  // Run
  def runPages = pageNode(run, Vector(scala, java, netLogo, r, scilab, packaged))

  lazy val run = DocumentationPage.fromScalatex(name = "Run", content = scalatex.documentation.run.Run, title = Some("Run Your Model"))
  lazy val scala = DocumentationPage.fromScalatex(name = "Scala", content = scalatex.documentation.run.Scala)
  lazy val java = DocumentationPage.fromScalatex(name = "Java", content = scalatex.documentation.run.Java)
  lazy val netLogo = DocumentationPage.fromScalatex(name = "NetLogo", content = scalatex.documentation.run.NetLogo)
  lazy val r = DocumentationPage.fromScalatex(name = "R", content = scalatex.documentation.run.R)
  lazy val scilab = DocumentationPage.fromScalatex(name = "Scilab", content = scalatex.documentation.run.Scilab)

  def packagedPages = pageNode(packaged, Vector(packagedPython, packagedCCplusplus))

  lazy val packaged = DocumentationPage.fromScalatex(name = "Package Native Code", content = scalatex.documentation.run.packaged.Packaged)
  lazy val packagedPython = DocumentationPage.fromScalatex(name = "Python", content = scalatex.documentation.run.packaged.PackagedPython)
  lazy val packagedCCplusplus = DocumentationPage.fromScalatex(name = "C Cplusplus", content = scalatex.documentation.run.packaged.PackagedCCplusplus, title = Some("C/C++"))

  // Explore
  def explorePages = pageNode(explore, Vector(directSampling, calibration, sensitivity, profile, pse))

  lazy val explore = DocumentationPage.fromScalatex(name = "Explore", content = scalatex.documentation.explore.Explore, title = Some("Explore Your Model"))

  def samplingPages = pageNode(directSampling, Vector(uniformSampling, csvSampling, lhsSampling, sobolSampling, oneFactorSampling, fileSampling, advancedSampling, spatialSampling))

  lazy val directSampling = DocumentationPage.fromScalatex(name = "Direct Sampling", content = scalatex.documentation.explore.sampling.DirectSampling)
  lazy val uniformSampling = DocumentationPage.fromScalatex(name = "Uniform Sampling", content = scalatex.documentation.explore.sampling.UniformSampling)
  lazy val csvSampling = DocumentationPage.fromScalatex(name = "CSV Sampling", content = scalatex.documentation.explore.sampling.CSVSampling)
  lazy val lhsSampling = DocumentationPage.fromScalatex(name = "LHS Sampling", content = scalatex.documentation.explore.sampling.LHSSampling)
  lazy val sobolSampling = DocumentationPage.fromScalatex(name = "Sobol Sampling", content = scalatex.documentation.explore.sampling.SobolSampling)
  lazy val oneFactorSampling = DocumentationPage.fromScalatex(name = "One Factor At A Time", content = scalatex.documentation.explore.sampling.OneFactorSampling)
  lazy val fileSampling = DocumentationPage.fromScalatex(name = "Sampling Over Files", content = scalatex.documentation.explore.sampling.FileSampling)
  lazy val advancedSampling = DocumentationPage.fromScalatex(name = "Advanced Samplings", content = scalatex.documentation.explore.sampling.AdvancedSampling)
  lazy val spatialSampling = DocumentationPage.fromScalatex(name = "Spatial Sampling", content = scalatex.documentation.explore.sampling.SpatialSampling)

  lazy val calibration = DocumentationPage.fromScalatex(name = "Calibration", content = scalatex.documentation.explore.Calibration)
  lazy val sensitivity = DocumentationPage.fromScalatex(name = "Sensitivity", content = scalatex.documentation.explore.Sensitivity, title = Some("Stastistical Sensitivity Analysis"))
  lazy val profile = DocumentationPage.fromScalatex(name = "Profile", content = scalatex.documentation.explore.Profile)
  lazy val pse = DocumentationPage.fromScalatex(name = "PSE", content = scalatex.documentation.explore.PSE, title = Some("Pattern Space Exploration"))

  // Scale
  def scalePages = pageNode(scale, Vector(multithread, ssh, cluster, egi, island))

  lazy val scale = DocumentationPage.fromScalatex(name = "Scale", content = scalatex.documentation.scale.Scale, title = Some("Scale on Different Environments"))
  lazy val multithread = DocumentationPage.fromScalatex(name = "Multithread", content = scalatex.documentation.scale.Multithread)
  lazy val ssh = DocumentationPage.fromScalatex(name = "SSH", content = scalatex.documentation.scale.SSH)
  lazy val cluster = DocumentationPage.fromScalatex(name = "Cluster", content = scalatex.documentation.scale.Cluster)
  lazy val egi = DocumentationPage.fromScalatex(name = "EGI", content = scalatex.documentation.scale.EGI, title = Some("European Grid Infrastructure"))
  lazy val island = DocumentationPage.fromScalatex(name = "Island", content = scalatex.documentation.scale.Island)

  // Language
  def languagePages = pageNode(language, Vector(fileManagement, scalaFunction, hook, transition, source, capsule, moleTask))

  lazy val language = DocumentationPage.fromScalatex(name = "Language", content = scalatex.documentation.language.Language, title = Some("The OpenMOLE Language"))
  lazy val fileManagement = DocumentationPage.fromScalatex(name = "File Management", content = scalatex.documentation.language.FileManagement)
  lazy val scalaFunction = DocumentationPage.fromScalatex(name = "Scala Functions", content = scalatex.documentation.language.ScalaFunction)
  lazy val hook = DocumentationPage.fromScalatex(name = "Hooks", content = scalatex.documentation.language.Hook)

  lazy val transition = DocumentationPage.fromScalatex(name = "Transitions", content = scalatex.documentation.language.advanced.Transition)
  lazy val source = DocumentationPage.fromScalatex(name = "Source", content = scalatex.documentation.language.advanced.Source)
  lazy val capsule = DocumentationPage.fromScalatex(name = "Capsule", content = scalatex.documentation.language.advanced.Capsule)
  lazy val moleTask = DocumentationPage.fromScalatex(name = "Mole Task", content = scalatex.documentation.language.advanced.MoleTask)

  // Advanced Concepts
  def advancedConceptsPages = pageNode(advancedConcepts, Vector(geneticAlgorithm, stochasticityManagement))

  //lazy val resumableWorkflow = DocumentationPage.fromScalatex(name = "Resumable Workflow", content = scalatex.documentation.advancedConcepts.ResumableWorkflow)

  lazy val advancedConcepts = DocumentationPage.fromScalatex(name = "Advanced concepts", content = scalatex.documentation.advancedConcepts.AdvancedConcepts)
  lazy val geneticAlgorithm = DocumentationPage.fromScalatex(name = "Genetic Algorithms", content = scalatex.documentation.advancedConcepts.GA.GeneticAlgorithm)
  lazy val stochasticityManagement = DocumentationPage.fromScalatex(name = "Stochasticity Management", content = scalatex.documentation.advancedConcepts.GA.StochasticityManagement)

  // Developers
  def developersPages = pageNode(developers, Vector(console, pluginDevelopment, webServer))

  lazy val developers = DocumentationPage.fromScalatex(name = "Developers", content = scalatex.documentation.developers.Developers, title = Some("Advanced Concepts for Developers"))
  lazy val console = DocumentationPage.fromScalatex(name = "Console Mode", content = scalatex.documentation.developers.Console)
  lazy val pluginDevelopment = DocumentationPage.fromScalatex(name = "Plugin Development", content = scalatex.documentation.developers.PluginDevelopment)
  lazy val webServer = DocumentationPage.fromScalatex(name = "RestAPI and Web Server", content = scalatex.documentation.developers.WebServer)

  // Tutorials
  def tutoPages = pageNode(tutorials, Vector(stepByStepIntro, exploreTuto, netLogoGA, simpleSAFire, market))
  //def menuTutoPages = Seq(stepByStepIntro, exploreTuto, netLogoGA, simpleSAFire, market)

  lazy val tutorials = DocumentationPage.fromScalatex(name = "Tutorials", content = scalatex.tutorials.Tutorials)
  lazy val stepByStepIntro = DocumentationPage.fromScalatex(name = "Step By Step Introduction", content = scalatex.tutorials.gettingStarted.StepByStepIntro, title = Some("A Step by Step Introduction to OpenMOLE"))
  lazy val exploreTuto = DocumentationPage.fromScalatex(name = "Explore Tutorial", content = scalatex.tutorials.gettingStarted.ExploreTuto, title = Some("How to Execute an Exploration Task"))
  lazy val netLogoGA = DocumentationPage.fromScalatex(name = "NetLogo GA", content = scalatex.tutorials.netLogo.NetLogoGA, title = Some("Using Genetic Algorithms to Calibrate a NetLogo Model"))
  lazy val simpleSAFire = DocumentationPage.fromScalatex(name = "Simple SA Fire", content = scalatex.tutorials.netLogo.SimpleSAFire, title = Some("Simple Sensitivity Analysis"))
  lazy val market = DocumentationPage.fromScalatex(name = "Market", content = scalatex.tutorials.Market, title = Some("Market Place"))

  // Community
  def communityPages = pageNode(OMcommunity, Vector(howToContribute, training, communications, whoWeAre, partner))

  lazy val OMcommunity = DocumentationPage.fromScalatex(name = "Community", content = scalatex.community.OMCommunity)
  lazy val howToContribute = DocumentationPage.fromScalatex(name = "How to Contribute", content = scalatex.community.HowToContribute)
  lazy val training = DocumentationPage.fromScalatex(name = "Trainings", content = scalatex.community.Training)
  lazy val communications = DocumentationPage.fromScalatex(name = "Publications", content = scalatex.community.Communications)
  lazy val whoWeAre = DocumentationPage.fromScalatex(name = "Who We Are", content = scalatex.community.WhoWeAre)
  lazy val partner = DocumentationPage.fromScalatex(name = "Our Partners", content = scalatex.community.Partner)

  // Download
  def downloadPages = pageNode(download, Vector(buildSources, releaseNotes))
  lazy val download = DocumentationPage.fromScalatex(name = "Download", content = scalatex.download.Download)
  lazy val buildSources = DocumentationPage.fromScalatex(name = "Build From Sources", content = scalatex.download.BuildSources)
  lazy val releaseNotes = DocumentationPage.fromScalatex(name = "Release Notes", content = scalatex.download.ReleaseNotes)

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
