
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

  val index = Page("index", scalatex.Index(), title = Some("OpenMOLE: scientific workflow, distributed computing, parameter tuning"))

  def gettingStarted = Page.fromScalatex("Getting started", scalatex.GettingStarted, title = Some("Getting started with OpenMOLE - introductory tutorial with a simple workflow"))

  def whoAreWe = Page("Who are we", scalatex.WhoAreWe(), title = Some("OpenMOLE Developers, reference publications, contact information"))

  def partner = Page("Partners", scalatex.Partner(), title = Some("OpenMOLE partners and collaborations"))

  val communications = Page.fromScalatex("Communications", scalatex.Communications, title = Some("Related scientific papers, conference slides, videos, OpenMOLE in the news"))

  def faq = Page.fromScalatex("faq", scalatex.FAQ, title = Some("FAQ"))

  def previousVersions = Page.fromScalatex("Previous versions", scalatex.PreviousVersions, title = Some("Previous versions of OpenMOLE"))

  val training = Page.fromScalatex("Trainings", scalatex.Training, title = Some("Live training sessions"))

  val all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, whoAreWe, partner, faq, communications, previousVersions, training)

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
      override def extraMenu = _extraMenu
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
  def extraMenu: Option[SideMenu]
  def source: Option[String]

  def anchor(name: String) = s"$file#${name.replaceAll(" ", "")}"
}

case class Parent[T](parent: Option[T])

object DocumentationPage {
  def fromScalatex[T <: Page.ScalatexContent](
    name:      String,
    content:   T,
    details:   ⇒ Seq[DocumentationPage] = Seq.empty,
    location:  Option[String]           = None,
    extraMenu: Option[SideMenu]         = None,
    title:     Option[String]           = None) = apply(name, content(), details, location, extraMenu, title, source = Some(content.sourcePath))

  def apply(
    name:      String,
    content:   ⇒ Frag,
    details:   ⇒ Seq[DocumentationPage] = Seq.empty,
    location:  Option[String]           = None,
    extraMenu: Option[SideMenu]         = None,
    title:     Option[String]           = None,
    source:    Option[String]           = None
  ) = {
    def _name = name
    def _content = content
    def _details = details
    def _location = location
    def _extraMenu = extraMenu
    def _title = title
    def _source = source

    new DocumentationPage {
      def name = _name
      def content = _content
      override def details = _details
      override def location = _location.getOrElse(name)
      override def extraMenu = _extraMenu
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
  index ⇒

  //var marketEntries: Seq[GeneratedMarketEntry] = Seq()

  def allPages = Vector[DocumentationPage](
    docSiteMap,
    gui,
    scala,
    java,
    r,
    native,
    nativePackaging,
    ccplusplus,
    packagedR,
    python,
    netLogo,
    mole,
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
    geneticalgo,
    island,
    stochasticity,
    profile,
    pse,
    tutorial,
    resume,
    headlessNetLogo,
    netLogoGA,
    capsule,
    dataflow,
    scalaFunction,
    plugin,
    webserver,
    dataProcessing,
    otherDoE,
    advancedConcepts,
    DirectSampling,
    advancedSampling,
    transition,
    hook,
    source,
    console
  )

  lazy val topPages = Seq(
    modelPages,
    methodPages,
    environmentPages
  ).flatten ++ Seq(model, method, environment)

  lazy val docSiteMap = DocumentationPage.fromScalatex(name = "Documentation Site Map", content = scalatex.documentation.DocSiteMap)

  lazy val gui = DocumentationPage.fromScalatex(name = "GUI guide", content = scalatex.documentation.GUI)

  def modelPages = Seq(scala, java, r, packagedR, python, ccplusplus, netLogo, mole, native)

  lazy val scala = DocumentationPage.fromScalatex(name = "Scala", content = scalatex.documentation.language.model.Scala, details = Seq(scalaFunction))
  lazy val java = DocumentationPage.fromScalatex(name = "Java", content = scalatex.documentation.language.model.Java)

  lazy val r = DocumentationPage.fromScalatex(name = "R", content = scalatex.documentation.language.model.R)
  lazy val native = DocumentationPage.fromScalatex(name = "Other Languages", content = scalatex.documentation.language.model.Native, details = Seq(nativePackaging), extraMenu = Some(SideMenu.nativeMenu))

  lazy val nativePackaging = DocumentationPage.fromScalatex(name = "Native Packaging", content = scalatex.documentation.details.NativePackaging, extraMenu = Some(SideMenu.nativePackagingMenu), title = Some("Native Code Packaging, CARE Task Options"))

  lazy val ccplusplus = DocumentationPage.fromScalatex(name = "C++", location = Some("cplusplus"), content = scalatex.documentation.language.model.CCplusplus, details = Seq(nativePackaging))
  lazy val packagedR = DocumentationPage.fromScalatex(name = "Packaged R", content = scalatex.documentation.language.model.PackagedR, details = Seq(nativePackaging))
  lazy val python = DocumentationPage.fromScalatex(name = "Python", content = scalatex.documentation.language.model.Python, details = Seq(nativePackaging))
  lazy val netLogo = DocumentationPage.fromScalatex(name = "NetLogo", content = scalatex.documentation.language.model.NetLogo)
  lazy val mole = DocumentationPage.fromScalatex(name = "Mole", content = scalatex.documentation.language.model.MoleTask)
  lazy val model = DocumentationPage.fromScalatex(name = "Models", content = scalatex.documentation.language.Model)

  def languagePages = Seq(model, environment, method)

  lazy val language = DocumentationPage.fromScalatex(name = "Language", content = scalatex.documentation.Language, title = Some("OpenMOLE Domain Specific Language"))

  lazy val transition = DocumentationPage.fromScalatex(name = "Transitions", content = scalatex.documentation.language.advanced.Transition)
  lazy val hook = DocumentationPage.fromScalatex(name = "Hooks", content = scalatex.documentation.language.advanced.Hook)
  lazy val source = DocumentationPage.fromScalatex(name = "Sources", content = scalatex.documentation.language.advanced.Source)
  lazy val capsule = DocumentationPage.fromScalatex(name = "Capsule", content = scalatex.documentation.language.advanced.Capsule)
  lazy val dataflow = DocumentationPage.fromScalatex(name = "Dataflow", content = scalatex.documentation.language.advanced.Dataflow)
  lazy val scalaFunction = DocumentationPage.fromScalatex(name = "Utility Scala functions", content = scalatex.documentation.language.advanced.ScalaFunction)

  lazy val environment = DocumentationPage.fromScalatex(name = "Environments", content = scalatex.documentation.language.Environment)

  lazy val multithread = DocumentationPage.fromScalatex(name = "Multi-threads", content = scalatex.documentation.language.environment.Multithread)
  lazy val ssh = DocumentationPage.fromScalatex(name = "SSH", content = scalatex.documentation.language.environment.SSH)
  lazy val egi = DocumentationPage.fromScalatex(name = "EGI", content = scalatex.documentation.language.environment.EGI)
  lazy val cluster = DocumentationPage.fromScalatex(
    name = "Clusters",
    content = scalatex.documentation.language.environment.Cluster,
    extraMenu = Some(SideMenu.clusterMenu)
  )

  def environmentPages = Seq(multithread, ssh, egi, cluster)

  def methodPages = Seq(DirectSampling, calibration, profile, pse, dataProcessing, otherDoE)

  lazy val tutorialPages = Seq(Pages.gettingStarted, netLogoGA, resume)

  lazy val detailsPages = Seq(
    geneticalgo,
    island,
    stochasticity,
    advancedSampling,
    nativePackaging,
    headlessNetLogo,
    gui,
    language
  )

  lazy val advancedPages = Seq(
    transition,
    hook,
    source,
    capsule,
    dataflow
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

  lazy val method = DocumentationPage.fromScalatex(name = "Methods", content = scalatex.documentation.language.Method, details = Seq(DocumentationPages.advancedSampling))

  lazy val calibration = DocumentationPage.fromScalatex(name = "Calibration", content = scalatex.documentation.language.method.Calibration, details = Seq(geneticalgo, island, stochasticity))
  lazy val geneticalgo = DocumentationPage.fromScalatex(name = "Genetic Algorithms", content = scalatex.documentation.details.GeneticAlgorithm)
  lazy val island = DocumentationPage.fromScalatex(name = "Islands Scheme", content = scalatex.documentation.details.Island, title = Some("Island repartition scheme"))
  lazy val stochasticity = DocumentationPage.fromScalatex(name = "Stochasticity management", content = scalatex.documentation.details.StochasticityManagement)

  lazy val profile = DocumentationPage.fromScalatex(name = "Profiles", content = scalatex.documentation.language.method.Profile)
  lazy val pse = DocumentationPage.fromScalatex(name = "PSE", content = scalatex.documentation.language.method.PSE)

  lazy val otherDoE = DocumentationPage.fromScalatex(name = "Other DoEs", content = scalatex.documentation.language.method.OtherDoE, extraMenu = Some(SideMenu.otherDoEMenu))

  lazy val DirectSampling = DocumentationPage.fromScalatex(name = "Direct Sampling", content = scalatex.documentation.language.method.DirectSampling, extraMenu = Some(SideMenu.directSamplingMenu))

  lazy val dataProcessing = DocumentationPage.fromScalatex(name = "Data Processing", content = scalatex.documentation.language.method.DataProcessing, extraMenu = Some(SideMenu.dataProcessingMenu))

  lazy val advancedConcepts = DocumentationPage.fromScalatex(name = "Advanced Concepts", content = scalatex.documentation.language.AdvancedConcepts)

  lazy val advancedSampling = DocumentationPage.fromScalatex(name = "Advanced Sampling", content = scalatex.documentation.language.advanced.AdvancedSampling)

  lazy val tutorial = DocumentationPage.fromScalatex(name = "Tutorials", content = scalatex.documentation.language.Tutorial)
  lazy val resume = DocumentationPage.fromScalatex(name = "Resume Workflow", content = scalatex.documentation.language.tutorial.Resume, title = Some("How to build a resumable workflow"))
  lazy val headlessNetLogo = DocumentationPage.fromScalatex(name = "Netlogo Headless", content = scalatex.documentation.language.advanced.HeadlessNetLogo, title = Some("Headless version of Netlog model"))
  lazy val netLogoGA = DocumentationPage.fromScalatex(name = "GA with NetLogo", content = scalatex.documentation.language.tutorial.NetLogoGA, title = Some("Calibrate a NetLogo model using genetic algorithms"))

  lazy val plugin = DocumentationPage.fromScalatex(name = "Plugin", content = scalatex.documentation.language.advanced.PluginDevelopment)
  lazy val webserver = DocumentationPage.fromScalatex(name = "Web Server", content = scalatex.documentation.development.WebServer, title = Some("Webserver and Rest API"))
  lazy val howToContribute = DocumentationPage.fromScalatex(name = "How to Contribute", content = scalatex.documentation.development.howToContribute, extraMenu = Some(SideMenu.howToContributeMenu))
  lazy val console = DocumentationPage.fromScalatex(name = "Console mode", content = scalatex.documentation.development.Console, extraMenu = Some(SideMenu.consoleMenu))

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
