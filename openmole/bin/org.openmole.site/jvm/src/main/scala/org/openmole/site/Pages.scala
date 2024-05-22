
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
//import com.github.rjeschke._
//import org.apache.commons.math3.genetics.GeneticAlgorithm

//import scalatex.{ openmole ⇒ scalatex }
import org.openmole.tool.file._

import scalatags.Text

import PageTree._
object Pages {

  val all: Vector[PageTree] = DocumentationPages.allPages.toVector

  //def rawFile(page: Page) = page.location.mkString("_") + ".html"
  def file(page: Page) = java.net.URLEncoder.encode(page.location, "UTF-8") + ".html"

  def isDoc(page: Page) = page match {
    case d: DocumentationPage ⇒ true
    case _                    ⇒ false
  }
}

object PageTree {
  implicit def fromPage(p: Page): PageLeaf = PageLeaf(p)

  implicit def fromSeqPage(ps: Seq[Page]): Seq[PageLeaf] = ps.map {
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
  def source: Option[(String)]
  def anchor(name: String) = s"$file#${name.replaceAll(" ", "")}"
}

object DocumentationPage {

  def fromContent(
    name: String,
    content: => PageContent,
    details: ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String] = None,
    title: Option[String] = None) =
    apply(name, content.content, details, location, title, source = Some(content.file.value.split("/").reverse.takeWhile(_ != "scala").reverse.mkString("/")))

  def apply(
    name:     String,
    content:  ⇒ Frag,
    details:  ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String]           = None,
    title:    Option[String]           = None,
    source:   => Option[String]           = None
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

object DocumentationPages:
  index ⇒

  def allPages = docPages.flatMap { _.sons } ++ tutoPages.sons ++ communityPages.sons ++ downloadPages.sons ++ headPages

  def headPages: Seq[PageTree] = docPages ++ Seq(plugPages, explorePages, scalePages, utilityTaskPages, languagePages, developersPages, tutoPages, communityPages, downloadPages)

  // Documentation
  lazy val documentation = DocumentationPage.fromContent(name = "Documentation", content = org.openmole.site.content.documentation.Documentation)

  def docPages = Seq(
    plugPages,
    explorePages,
    samplingPages,
    scalePages,
    utilityTaskPages,
    languagePages,
    advancedConceptsPages,
    developersPages,
    docLonelyPages
  )

  def docLonelyPages = pageNode(documentation, Vector(gui, commandOptions, faq))

  lazy val gui = DocumentationPage.fromContent(name = "GUI", content = org.openmole.site.content.documentation.GUI, title = Some("Graphical User Interface"))
  lazy val commandOptions = DocumentationPage.fromContent(name = "Command Options", content = org.openmole.site.content.documentation.CommandOptions)
  val faq = DocumentationPage.fromContent(name = "FAQ", content = org.openmole.site.content.FAQ, title = Some("Frequently Asked Questions"))

  // Plug
  def plugPages = pageNode(plug, Vector(scala, java, python, r, netLogo, gama, scilab, julia, container))

  lazy val plug = DocumentationPage.fromContent(name = "Plug", content = org.openmole.site.content.plug.Plug, title = Some("Plug Your Model"))
  lazy val scala = DocumentationPage.fromContent(name = "Scala", content = org.openmole.site.content.plug.Scala)
  lazy val java = DocumentationPage.fromContent(name = "Java", content = org.openmole.site.content.plug.Java)
  lazy val netLogo = DocumentationPage.fromContent(name = "NetLogo", content = org.openmole.site.content.plug.NetLogo)
  lazy val python = DocumentationPage.fromContent(name = "Python", content = org.openmole.site.content.plug.Python)
  lazy val r = DocumentationPage.fromContent(name = "R", content = org.openmole.site.content.plug.R)
  lazy val scilab = DocumentationPage.fromContent(name = "Scilab", content = org.openmole.site.content.plug.Scilab)
  lazy val gama = DocumentationPage.fromContent(name = "GAMA", content = org.openmole.site.content.plug.GAMA)
  lazy val julia = DocumentationPage.fromContent(name = "Julia", content = org.openmole.site.content.plug.Julia)
  lazy val container = DocumentationPage.fromContent(name = "Any Other Executable", content = org.openmole.site.content.plug.Container)

  // Explore
  def explorePages = pageNode(explore, Vector(samplings, calibration, sensitivity, profile, pse, ose, abc))

  lazy val explore = DocumentationPage.fromContent(name = "Explore", content = org.openmole.site.content.explore.Explore, title = Some("Explore Your Model"))

  def samplingPages = pageNode(samplings, Vector(elementarySamplings, highDimensionSamplings, uniformSampling, fileSampling, customSampling, advancedSampling, aggregationSampling))

  lazy val samplings = DocumentationPage.fromContent(name = "Samplings", content = org.openmole.site.content.explore.sampling.Samplings)
  lazy val elementarySamplings = DocumentationPage.fromContent(name = "Elementary Samplings", content = org.openmole.site.content.explore.sampling.ElementarySampling)
  //lazy val gridSampling = DocumentationPage.fromScalatex(name = "Grid Sampling", content = scalatex.documentation.explore.sampling.GridSampling)
  //lazy val oneFactorSampling = DocumentationPage.fromScalatex(name = "One Factor at a Time", content = scalatex.documentation.explore.sampling.OneFactorSampling)
  lazy val uniformSampling = DocumentationPage.fromContent(name = "Uniform Sampling", content = org.openmole.site.content.explore.sampling.UniformSampling)
  lazy val highDimensionSamplings = DocumentationPage.fromContent(name = "High Dimension Samplings", content = org.openmole.site.content.explore.sampling.HighDimensionSampling, title = Some("Samplings for High Dimension Spaces"))
  //  lazy val lhsSampling = DocumentationPage.fromScalatex(name = "LHS Sampling", content = scalatex.documentation.explore.sampling.LHSSampling)
  //  lazy val sobolSampling = DocumentationPage.fromScalatex(name = "Sobol Sampling", content = scalatex.documentation.explore.sampling.SobolSampling)
  lazy val customSampling = DocumentationPage.fromContent(name = "Custom Sampling", content = org.openmole.site.content.explore.sampling.CustomSampling)
  lazy val fileSampling = DocumentationPage.fromContent(name = "Sampling Over Files", content = org.openmole.site.content.explore.sampling.FileSampling)
  lazy val advancedSampling = DocumentationPage.fromContent(name = "Operations on Samplings", content = org.openmole.site.content.explore.sampling.AdvancedSampling, title = Some("Advanced Operations on Samplings"))
  lazy val aggregationSampling = DocumentationPage.fromContent(name = "Aggregate Sampling Results", content = org.openmole.site.content.explore.sampling.Aggregation)

  lazy val calibration = DocumentationPage.fromContent(name = "Calibration", content = org.openmole.site.content.explore.Calibration)
  lazy val sensitivity = DocumentationPage.fromContent(name = "Sensitivity", content = org.openmole.site.content.explore.Sensitivity, title = Some("Stastistical Sensitivity Analysis"))
  lazy val profile = DocumentationPage.fromContent(name = "Profile", content = org.openmole.site.content.explore.Profile)
  lazy val pse = DocumentationPage.fromContent(name = "PSE", content = org.openmole.site.content.explore.PSE, title = Some("Pattern Space Exploration"))
  lazy val ose = DocumentationPage.fromContent(name = "OSE", content = org.openmole.site.content.explore.OSE, title = Some("Origin Space Exploration"))
  lazy val abc = DocumentationPage.fromContent(name = "ABC", content = org.openmole.site.content.explore.ABC, title = Some("Approximate Bayesian computation"))

  // Scale
  def scalePages = pageNode(scale, Vector(multithread, ssh, cluster, egi, dispatch))

  lazy val scale = DocumentationPage.fromContent(name = "Scale Up", content = org.openmole.site.content.scale.Scale, title = Some("Scale Up Your Experiments"))
  lazy val multithread = DocumentationPage.fromContent(name = "Multithread", content = org.openmole.site.content.scale.Multithread)
  lazy val ssh = DocumentationPage.fromContent(name = "SSH", content = org.openmole.site.content.scale.SSH)
  lazy val cluster = DocumentationPage.fromContent(name = "Cluster", content = org.openmole.site.content.scale.Cluster)
  lazy val egi = DocumentationPage.fromContent(name = "EGI", content = org.openmole.site.content.scale.EGI, title = Some("European Grid Infrastructure"))
  lazy val dispatch = DocumentationPage.fromContent(name = "Dispatch", content = org.openmole.site.content.scale.Dispatch, title = Some("Dispatch Jobs on Multiple Environments"))

  // Language
  def languagePages = pageNode(language, Vector(fileManagement, scalaFunction, hook, transition, source, capsule))

  lazy val language = DocumentationPage.fromContent(name = "Language", content = org.openmole.site.content.language.Language, title = Some("The OpenMOLE Language"))
  lazy val fileManagement = DocumentationPage.fromContent(name = "File Management", content = org.openmole.site.content.language.FileManagement)
  lazy val scalaFunction = DocumentationPage.fromContent(name = "Scala Functions", content = org.openmole.site.content.language.ScalaFunction)
  lazy val hook = DocumentationPage.fromContent(name = "Hooks", content = org.openmole.site.content.language.Hook)

  lazy val transition = DocumentationPage.fromContent(name = "Transitions", content = org.openmole.site.content.language.advanced.Transition)
  lazy val source = DocumentationPage.fromContent(name = "Source", content = org.openmole.site.content.language.advanced.Source)
  lazy val capsule = DocumentationPage.fromContent(name = "Capsule", content = org.openmole.site.content.language.advanced.Capsule)

  def utilityTaskPages = pageNode(utilityTask, Vector(templateTask, moleTask, tryTask, spatialTask))

  lazy val utilityTask = DocumentationPage.fromContent(name = "Utility Tasks", content = org.openmole.site.content.documentation.utilityTask.Task)
  lazy val templateTask = DocumentationPage.fromContent(name = "Template Task", content = org.openmole.site.content.documentation.utilityTask.TemplateTask)
  lazy val moleTask = DocumentationPage.fromContent(name = "Mole Task", content = org.openmole.site.content.documentation.utilityTask.MoleTask)
  lazy val tryTask = DocumentationPage.fromContent(name = "Try Task", content = org.openmole.site.content.documentation.utilityTask.TryTask)
  lazy val spatialTask = DocumentationPage.fromContent(name = "Spatial Task", content = org.openmole.site.content.documentation.utilityTask.SpatialTask)

  // Advanced Concepts
  def advancedConceptsPages = pageNode(advancedConcepts, Vector(geneticAlgorithm, island, stochasticityManagement))

  //lazy val resumableWorkflow = DocumentationPage.fromScalatex(name = "Resumable Workflow", content = scalatex.documentation.advancedConcepts.ResumableWorkflow)

  lazy val advancedConcepts = DocumentationPage.fromContent(name = "Advanced Concepts", content = org.openmole.site.content.advancedConcepts.AdvancedConcepts)
  lazy val geneticAlgorithm = DocumentationPage.fromContent(name = "Genetic Algorithms", content = org.openmole.site.content.advancedConcepts.GA.GeneticAlgorithm)
  lazy val island = DocumentationPage.fromContent(name = "Island Distribution Scheme", content = org.openmole.site.content.advancedConcepts.GA.Island)
  lazy val stochasticityManagement = DocumentationPage.fromContent(name = "Stochasticity Management", content = org.openmole.site.content.advancedConcepts.GA.StochasticityManagement)

  // Developers
  def developersPages = pageNode(developers, Vector(console, pluginDevelopment, extensionAPI, restAPI, documentationGen))

  lazy val developers = DocumentationPage.fromContent(name = "Developers", content = org.openmole.site.content.developers.Developers, title = Some("Advanced Concepts for Developers"))
  lazy val console = DocumentationPage.fromContent(name = "Console Mode", content = org.openmole.site.content.developers.Console)
  lazy val pluginDevelopment = DocumentationPage.fromContent(name = "Plugin Development", content = org.openmole.site.content.developers.PluginDevelopment)
  lazy val restAPI = DocumentationPage.fromContent(name = "Rest API", content = org.openmole.site.content.developers.RESTAPI)
  lazy val extensionAPI = DocumentationPage.fromContent(name = "Extension API", content = org.openmole.site.content.developers.ExtensionAPI)
  lazy val documentationGen = DocumentationPage.fromContent(name = "Documentation generation", content = org.openmole.site.content.developers.DocumentationGen)

  // Tutorials
  def tutoPages = pageNode(tutorials, Vector(stepByStepIntro, exploreTuto, simpleSAFire, netLogoGA, market))
  //def menuTutoPages = Seq(stepByStepIntro, exploreTuto, netLogoGA, simpleSAFire, market)

  lazy val tutorials = DocumentationPage.fromContent(name = "Tutorials", content = org.openmole.site.content.tutorials.Tutorials)
  lazy val stepByStepIntro = DocumentationPage.fromContent(name = "Step By Step Introduction", content = org.openmole.site.content.tutorials.gettingStarted.StepByStepIntro, title = Some("A Step by Step Introduction to OpenMOLE"))
  lazy val exploreTuto = DocumentationPage.fromContent(name = "First Exploration", content = org.openmole.site.content.tutorials.gettingStarted.ExploreTuto, title = Some("How to Execute an Exploration Task"))
  lazy val simpleSAFire = DocumentationPage.fromContent(name = "Sensitivity Analysis", content = org.openmole.site.content.tutorials.netlogo.SimpleSAFire, title = Some("Simple Sensitivity Analysis of the Fire NetLogo Model"))
  lazy val netLogoGA = DocumentationPage.fromContent(name = "Calibration with GA", content = org.openmole.site.content.tutorials.netlogo.NetLogoGA, title = Some("Using Genetic Algorithms to Calibrate a NetLogo Model"))
  lazy val market = DocumentationPage.fromContent(name = "Market Place", content = org.openmole.site.content.tutorials.Market)

  // Community
  def communityPages = pageNode(OMcommunity, Vector(howToContribute, training, communications, whoWeAre, partner))

  lazy val OMcommunity = DocumentationPage.fromContent(name = "Community", content = org.openmole.site.content.community.OMCommunity)
  lazy val howToContribute = DocumentationPage.fromContent(name = "How to Contribute", content = org.openmole.site.content.community.HowToContribute)
  lazy val training = DocumentationPage.fromContent(name = "Trainings", content = org.openmole.site.content.community.Training)
  lazy val communications = DocumentationPage.fromContent(name = "Publications", content = org.openmole.site.content.community.Publications)
  lazy val whoWeAre = DocumentationPage.fromContent(name = "Who We Are", content = org.openmole.site.content.community.WhoWeAre)
  lazy val partner = DocumentationPage.fromContent(name = "Our Partners", content = org.openmole.site.content.community.Partner)

  // Download
  def downloadPages = pageNode(download, Vector(buildSources, releaseNotes))
  lazy val download = DocumentationPage.fromContent(name = "Download", content = org.openmole.site.content.download.Download)
  lazy val buildSources = DocumentationPage.fromContent(name = "Build From Sources", content = org.openmole.site.content.download.BuildSources)
  lazy val releaseNotes = DocumentationPage.fromContent(name = "Release Notes", content = org.openmole.site.content.download.ReleaseNotes)

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



case class PageContent(content: Frag*)(implicit val file: sourcecode.File)