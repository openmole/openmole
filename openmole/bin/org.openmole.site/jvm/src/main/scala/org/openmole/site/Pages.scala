
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

  def index = Page("index", scalatex.Index(), title = Some("OpenMOLE: scientific workflow, distributed computing, parameter tuning"))

  def gettingStarted = Page("getting_started", scalatex.GettingStarted(), title = Some("Getting started with OpenMOLE - introductory tutorial"))

  def whoAreWe = Page("who_are_we", scalatex.WhoAreWe(), title = Some("Developers, reference publications, contact information - OpenMOLE"))

  def communications = Page("communications", scalatex.Communications(), title = Some("Related papers, conference slides, videos, OpenMOLE in the news"))

  def faq = Page("faq", scalatex.FAQ(), title = Some("FAQ"))

  def previousVersions = Page("previous_versions", scalatex.PreviousVersions(), title = Some("Previous versions"))

  def all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, whoAreWe, faq, communications, previousVersions)

  //def rawFile(page: Page) = page.location.mkString("_") + ".html"
  def file(page: Page) = java.net.URLEncoder.encode(page.location, "UTF-8") + ".html"

  def isDoc(page: Page) = page match {
    case d: DocumentationPage ⇒ true
    case _                    ⇒ false
  }

  //  def isTopDoc(page: Page) = DocumentationPages.topPages.map {
  //    _.children
  //  }

}

object Page {
  def apply(name: String, content: Frag, details: Seq[Page] = Seq(), title: Option[String] = None) = {
    val (_name, _content, _details, _title) = (name, content, details, title)

    new Page {
      override def name: String = _name
      override def content = _content
      override def title = _title
      override def details = _details
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
}

case class Parent[T](parent: Option[T])

object DocumentationPage {
  def apply(
    name:     String,
    content:  ⇒ Frag,
    details:  ⇒ Seq[DocumentationPage] = Seq.empty,
    location: Option[String]           = None
  ) = {
    def _name = name
    def _content = content
    def _details = details
    def _location = location
    new DocumentationPage {
      def name = _name
      def content = _content
      override def details = _details
      override def location = _location.getOrElse(name)
    }
  }
}

abstract class DocumentationPage extends Page {

  //  def allPages: Seq[Page] = {
  //    {
  //      def pages(p: DocumentationPage): List[Page] =
  //        p.children.toList ::: p.details.toList ::: p.children.flatMap(_.allPages).toList
  //      this :: pages(this)
  //    }.distinct
  //  }

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
    documentation,
    application,
    migration,
    scala,
    java,
    native,
    nativeAPI,
    nativePackaging,
    CARETroubleshooting,
    ccplusplus,
    rscript,
    python,
    netLogo,
    mole,
    model,
    language,
    sampling,
    transition,
    hook,
    source,
    environment,
    multithread,
    ssh,
    egi,
    cluster,
    desktopGrid,
    method,
    calibration,
    geneticalgo,
    island,
    stochasticity,
    sensitivity,
    profile,
    pse,
    helloWorld,
    tutorial,
    resume,
    headlessNetLogo,
    netLogoGA,
    capsule,
    development,
    compilation,
    documentationWebsite,
    plugin,
    branching,
    webserver
  )

  lazy val topPages = Seq(
    modelPages,
    methodPages,
    environmentPages
  ).flatten

  //  lazy val topPagesChildren = topPages.flatMap {
  //    _.children
  //  }.distinct

  /* Application */

  lazy val documentation = DocumentationPage(name = "Documentation", content = scalatex.documentation.Documentation())

  lazy val application = DocumentationPage(name = "Application", content = scalatex.documentation.Application())
  lazy val migration = DocumentationPage(name = "Migration", content = scalatex.documentation.application.Migration())

  def modelPages = Seq(scala, java, rscript, python, ccplusplus, native, netLogo, mole)

  lazy val scala = DocumentationPage(name = "Scala", content = scalatex.documentation.language.model.Scala())
  lazy val java = DocumentationPage(name = "Java", content = scalatex.documentation.language.model.Java())

  lazy val native = DocumentationPage(
    name = "Native",
    content = scalatex.documentation.language.model.Native(),
    details = Seq(nativeAPI, nativePackaging, CARETroubleshooting)
  )

  lazy val nativeAPI = DocumentationPage(name = "Native API", content = scalatex.documentation.details.NativeAPI())
  lazy val nativePackaging = DocumentationPage(name = "Native Packaging", content = scalatex.documentation.details.NativePackaging())
  lazy val CARETroubleshooting = DocumentationPage(name = "CARE Troubleshooting", content = scalatex.documentation.details.CARETroubleShooting())

  lazy val ccplusplus = DocumentationPage(name = "C++", location = Some("cplusplus"), content = scalatex.documentation.language.model.CCplusplus(), details = Seq(nativeAPI, nativePackaging, CARETroubleshooting))
  lazy val rscript = DocumentationPage(name = "R Script", content = scalatex.documentation.language.model.RScript(), details = Seq(nativeAPI, nativePackaging, CARETroubleshooting))
  lazy val python = DocumentationPage(name = "Python", content = scalatex.documentation.language.model.Python(), details = Seq(nativeAPI, nativePackaging, CARETroubleshooting))
  lazy val netLogo = DocumentationPage(name = "NetLogo", content = scalatex.documentation.language.model.NetLogo())
  lazy val mole = DocumentationPage(name = "Mole", content = scalatex.documentation.language.model.MoleTask())
  lazy val model = DocumentationPage(name = "Models", content = scalatex.documentation.language.Model())

  def languagePages = Seq(model, sampling, transition, hook, environment, source, method)

  lazy val language = DocumentationPage(name = "Language", content = scalatex.documentation.Language())

  lazy val sampling = DocumentationPage(name = "Samplings", content = scalatex.documentation.language.Sampling())
  lazy val transition = DocumentationPage(name = "Transitions", content = scalatex.documentation.language.Transition())
  lazy val hook = DocumentationPage(name = "Hooks", content = scalatex.documentation.language.Hook())
  lazy val source = DocumentationPage(name = "Sources", content = scalatex.documentation.language.Source())

  def environmentPages = Seq(multithread, ssh, egi, cluster, desktopGrid)

  lazy val environment = DocumentationPage(name = "Environments", content = scalatex.documentation.language.Environment())

  lazy val multithread = DocumentationPage(name = "Multi-threads", content = scalatex.documentation.language.environment.Multithread())
  lazy val ssh = DocumentationPage(name = "SSH", content = scalatex.documentation.language.environment.SSH())
  lazy val egi = DocumentationPage(name = "EGI", content = scalatex.documentation.language.environment.EGI())
  lazy val cluster = DocumentationPage(name = "Clusters", content = scalatex.documentation.language.environment.Cluster())

  lazy val desktopGrid = DocumentationPage(name = "DesktopGrid", content = scalatex.documentation.language.environment.DesktopGrid())

  def methodPages = Seq(calibration, sensitivity, profile, pse, dataProcessing, otherDoE)

  lazy val method = DocumentationPage(name = "Methods", content = scalatex.documentation.language.Method())

  lazy val calibration = DocumentationPage(name = "Calibration", content = scalatex.documentation.language.method.Calibration(), details = Seq(geneticalgo, island, stochasticity))

  lazy val geneticalgo = DocumentationPage(name = "Genetic Algorithms", content = scalatex.documentation.details.GeneticAlgorithm())
  lazy val island = DocumentationPage(name = "Islands Scheme", content = scalatex.documentation.details.Island())
  lazy val stochasticity = DocumentationPage(name = "Stochasticity Management", content = scalatex.documentation.details.StochasticityManagement())

  lazy val sensitivity = DocumentationPage(name = "Sensitivity Analysis", content = scalatex.documentation.language.method.Sensitivity())
  lazy val profile = DocumentationPage(name = "Profiles", content = scalatex.documentation.language.method.Profile())
  lazy val pse = DocumentationPage(name = "PSE", content = scalatex.documentation.language.method.PSE())
  lazy val otherDoE = DocumentationPage(name = "other DoE", content = scalatex.documentation.language.method.OtherDoE())
  lazy val dataProcessing = DocumentationPage(name = "Data Processing", content = scalatex.documentation.language.method.DataProcessing())

  lazy val helloWorld = DocumentationPage(name = "Hello Word!", content = Pages.gettingStarted.content)

  def tutorialPages = Seq(helloWorld, resume, headlessNetLogo, netLogoGA, capsule) //++ marketTutorials

  lazy val tutorial = DocumentationPage(name = "Tutorials", content = scalatex.documentation.language.Tutorial())
  lazy val resume = DocumentationPage(name = "Resume Workflow", content = scalatex.documentation.language.tutorial.Resume())
  lazy val headlessNetLogo = DocumentationPage(name = "Netlogo Headless", content = scalatex.documentation.language.tutorial.HeadlessNetLogo())
  lazy val netLogoGA = DocumentationPage(name = "GA with NetLogo", content = scalatex.documentation.language.tutorial.NetLogoGA())
  lazy val capsule = DocumentationPage(name = "Capsule", content = scalatex.documentation.language.tutorial.Capsule())

  def developmentPages = Seq(compilation, documentationWebsite, plugin, branching, webserver)

  lazy val development = DocumentationPage(name = "Development", content = scalatex.documentation.Development())
  lazy val compilation = DocumentationPage(name = "Compilation", content = scalatex.documentation.development.Compilation())
  lazy val documentationWebsite = DocumentationPage(name = "Documentation", content = scalatex.documentation.development.DocumentationWebsite())
  lazy val plugin = DocumentationPage(name = "Plugins", content = scalatex.documentation.development.Plugin())
  lazy val branching = DocumentationPage(name = "Branching model", content = scalatex.documentation.development.Branching())
  lazy val webserver = DocumentationPage(name = "Web Server", content = scalatex.documentation.development.WebServer())

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
