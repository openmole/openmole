
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

import org.openmole.site.market._

import scalatags.Text.all._
import com.github.rjeschke._
import org.openmole.site.market.Market.Tags

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

  def all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, whoAreWe, faq, communications)

  def file(page: Page) = page.location.mkString("_") + ".html"

  def isDoc(page: Page) = page match {
    case d: DocumentationPage ⇒ true
    case _                    ⇒ false
  }

  def isTopDoc(page: Page) = DocumentationPages.topPages.map {
    _.children
  }

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

case class PageIntro(intro: scalatags.Text.all.Frag, more: Option[scalatags.Text.all.Frag] = None)

trait Page {
  def content: Frag

  def name: String

  def id: String = name

  def title: Option[String]

  def location = Seq(id)

  def file = Pages.file(this)

  def details: Seq[Page]

  def intro: Option[PageIntro] = None
}

case class Parent[T](parent: Option[T])

abstract class DocumentationPage(implicit p: Parent[DocumentationPage] = Parent(None)) extends Page {
  def parent = p.parent

  implicit def thisIsParent = Parent[DocumentationPage](Some(this))

  def content: Frag

  def name: String

  def children: Seq[DocumentationPage]

  def title: Option[String] = None

  override def location: Seq[String] =
    parent match {
      case None    ⇒ Seq(id)
      case Some(p) ⇒ p.location ++ Seq(id)
    }

  def allPages: Seq[Page] = {
    {
      def pages(p: DocumentationPage): List[Page] =
        p.children.toList ::: p.details.toList ::: p.children.flatMap(_.allPages).toList
      this :: pages(this)
    }.distinct
  }

  override def equals(o: scala.Any): Boolean =
    o match {
      case p2: DocumentationPage ⇒ this.location == p2.location
      case _                     ⇒ false
    }

  override def hashCode(): Int = location.hashCode()
}

object DocumentationPages {
  index ⇒

  var marketEntries: Seq[GeneratedMarketEntry] = Seq()

  def apply(
    name:     String,
    content:  Frag,
    children: Seq[DocumentationPage] = Seq.empty,
    details:  Seq[DocumentationPage] = Seq.empty,
    location: Option[Seq[String]]    = None,
    intro:    Option[PageIntro]      = None
  )(implicit p: Parent[DocumentationPage] = Parent(None)) = {
    val (_name, _content, _details, _children, _location, _intro) = (name, content, details, children, location, intro)
    new DocumentationPage {
      override def children = _children

      override def name = _name

      override def content = _content

      override def details = _details

      override def location = _location.getOrElse(super.location)

      override def intro = _intro
    }
  }

  def allPages = root.allPages

  lazy val topPages = Seq(
    root.language.model,
    root.language.method,
    root.language.environment
  )

  lazy val topPagesChildren = topPages.flatMap {
    _.children
  }.distinct

  val root = new DocumentationPage {
    def name = "Documentation"

    override def title = Some(name)

    def content = scalatex.documentation.Documentation()

    def details = Seq()

    def children = Seq(application, language, tutorial, market, development)

    val application = new DocumentationPage {
      def name = "Application"

      override def title = Some(name)

      def children = Seq(migration)

      def content = scalatex.documentation.Application()

      def details = Seq()

      val migration = new DocumentationPage() {
        def children: Seq[DocumentationPage] = Seq()

        def name: String = "Migration"

        override def title = Some(name)

        def content = scalatex.documentation.application.Migration()

        def details = Seq()
      }
    }

    val language =
      new DocumentationPage {
        def name = "Language"

        override def title = Some(name)

        def children = Seq(model, sampling, transition, hook, environment, source, method)

        def content = scalatex.documentation.Language()

        def details = Seq()

        val model = new DocumentationPage {
          def name = "Models"

          override def title = Some(name)

          def children = Seq(scala, java, rscript, python, /*ccplusplus,*/ native, netLogo, mole)

          def content = scalatex.documentation.language.Model()

          def details = Seq()

          lazy val modelIntro = Some(PageIntro(scalatex.documentation.language.ModelIntro(), Some(scalatex.documentation.language.Model())))

          val scala = new DocumentationPage {
            def name = "Scala"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.model.Scala()

            override def intro = modelIntro
          }

          val java = new DocumentationPage {
            def name = "Java"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.model.Java()

            override def intro = modelIntro
          }

          val native = new DocumentationPage {
            def name = "Native"

            override def title = Some(name)

            def children = Seq()

            def details = Seq(nativeAPI, nativePackaging, CARETroubleshooting)

            def content = scalatex.documentation.language.model.Native()

            override def intro = modelIntro
          }

          val ccplusplus = new DocumentationPage {
            def name = "C/C++"

            override def title = Some(name)

            def children = Seq()

            def details = Seq(nativeAPI, nativePackaging, CARETroubleshooting)

            def content = scalatex.documentation.language.model.CCplusplus()

            override def intro = modelIntro
          }

          val rscript = new DocumentationPage {
            def name = "R Script"

            override def title = Some(name)

            def children = Seq()

            def details = Seq(nativeAPI, nativePackaging, CARETroubleshooting)

            def content = scalatex.documentation.language.model.RScript()

            override def intro = modelIntro
          }

          val python = new DocumentationPage {
            def name = "Python"

            override def title = Some(name)

            def children = Seq()

            def details = Seq(nativeAPI, nativePackaging, CARETroubleshooting)

            def content = scalatex.documentation.language.model.Python()

            override def intro = modelIntro
          }

          val netLogo = new DocumentationPage {
            def name = "NetLogo"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.model.NetLogo()

            override def intro = modelIntro
          }

          val mole = new DocumentationPage {
            def name = "Mole"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.model.MoleTask()

            override def intro = modelIntro
          }

          //details
          val nativeAPI = new DocumentationPage {
            override def id = "NativeAPI"

            def name = "API"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.details.NativeAPI()
          }

          val nativePackaging = new DocumentationPage {
            override def id = "NativePackaging"

            def name = "Native Packaging"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.details.NativePackaging()
          }

          //troubleshooting care
          val CARETroubleshooting = new DocumentationPage {
            override def id = "CARETroubleshooting"

            def name = "CARE Troubleshooting"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.details.CARETroubleShooting()
          }
        }

        val sampling = new DocumentationPage {
          def name = "Samplings"

          override def title = Some(name)

          def children = Seq()

          def details = Seq()

          def content = scalatex.documentation.language.Sampling()
        }

        val transition = new DocumentationPage {
          def name = "Transitions"

          override def title = Some(name)

          def children = Seq()

          def details = Seq()

          def content = scalatex.documentation.language.Transition()
        }

        val hook = new DocumentationPage {
          def name = "Hooks"

          override def title = Some(name)

          def children = Seq()

          def details = Seq()

          def content = scalatex.documentation.language.Hook()
        }

        val environment = new DocumentationPage {
          def name = "Environments"

          override def title = Some(name)

          def children = Seq(multithread, ssh, egi, cluster, desktopGrid)

          def content = scalatex.documentation.language.Environment()

          def details = Seq()

          lazy val envIntro = Some(PageIntro(scalatex.documentation.language.environment.EnvironmentIntro(), Some(scalatex.documentation.language.Environment())))

          val multithread = new DocumentationPage {
            override def id = "MultiThread"

            def name = "Multi-threads"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.environment.Multithread()

            override def intro = envIntro
          }

          val ssh = new DocumentationPage {
            def name = "SSH"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.environment.SSH()

            override def intro = envIntro
          }

          val egi = new DocumentationPage {
            def name = "EGI"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.environment.EGI()

            override def intro = envIntro
          }

          val cluster = new DocumentationPage {
            def name = "Clusters"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.environment.Cluster()

            override def intro = envIntro
          }

          val desktopGrid = new DocumentationPage {
            override def id = "DesktopGrid"

            def name = "Desktop Grid"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.environment.DesktopGrid()

            override def intro = envIntro
          }

        }

        def source = new DocumentationPage {
          def name = "Sources"

          override def title = Some(name)

          def children = Seq()

          def details = Seq()

          def content = scalatex.documentation.language.Source()
        }

        def method = new DocumentationPage {
          def name = "Methods"

          override def title = Some(name)

          def children = Seq(pse, profile)

          def details = Seq()

          lazy val methIntro = Some(PageIntro(scalatex.documentation.language.method.MethodIntro(), Some(scalatex.documentation.language.Method())))

          def content = scalatex.documentation.language.Method()

          def pse = new DocumentationPage {
            def name = "PSE"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.method.PSE()

            override def intro = methIntro
          }

          def profile = new DocumentationPage {
            def name = "Profiles"

            override def title = Some(name)

            def children = Seq()

            def details = Seq()

            def content = scalatex.documentation.language.method.Profile()

            override def intro = methIntro
          }
        }
      }

    lazy val tutorial = new DocumentationPage {
      def name = "Tutorials"

      override def title = Some(name)

      def children = Seq(helloWorld, resume, headlessNetLogo, netLogoGA, capsule)

      def details = Seq()

      def content = scalatex.documentation.language.Tutorial()

      marketEntries.filter(_.tags.exists(_ == Tags.tutorial)).flatMap(MD.generatePage(_))

      val helloWorld = new DocumentationPage {
        override def id = "HelloWord"

        def name = "Hello World"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = Pages.gettingStarted.content
      }

      val resume = new DocumentationPage {
        override def id = "ResumeWorkflow"

        def name = "Resume workflow"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.language.tutorial.Resume()
      }

      val headlessNetLogo = new DocumentationPage {
        override def id = "NetlogoHeadless"

        def name = "NetLogo Headless"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.language.tutorial.HeadlessNetLogo()
      }

      val netLogoGA = new DocumentationPage {
        override def id = "GAwithNetLogo"

        def name = "GA with NetLogo"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.language.tutorial.NetLogoGA()
      }

      val capsule = new DocumentationPage {
        def name = "Capsule"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.language.tutorial.Capsule()
      }
    }

    val market = new DocumentationPage {
      override def content: Text.all.Frag = div(tagContent(marketEntries))

      override def children: Seq[DocumentationPage] = Seq()

      override def name: String = "Market"

      override def details: Seq[Page] = Seq()

      def tagContent(entries: Seq[GeneratedMarketEntry]) =
        ul(
          entries.sortBy(_.entry.name.toLowerCase).map {
            de ⇒ li(entryContent(de))
          }: _*
        )

      def entryContent(deployedMarketEntry: GeneratedMarketEntry) = {
        def title: Modifier =
          deployedMarketEntry.viewURL match {
            case None    ⇒ deployedMarketEntry.entry.name
            case Some(l) ⇒ a(deployedMarketEntry.entry.name, href := l)
          }

        def content =
          Seq[Modifier](
            deployedMarketEntry.readme.map {
              rm ⇒ RawFrag(txtmark.Processor.process(rm))
            }.getOrElse(p("No README.md available yet.")),
            a("Packaged archive", href := deployedMarketEntry.archive), " (can be imported in OpenMOLE)"
          ) ++ deployedMarketEntry.viewURL.map(u ⇒ br(a("Source repository", href := u)))

        div(scalatags.Text.all.id := "market-entry")(content: _*)
      }

      def themes: Seq[Market.Tag] = {
        marketEntries.flatMap(_.entry.tags).distinct.sortBy(_.label.toLowerCase)
      }

    }

    def development = new DocumentationPage {
      def name = "Development"

      override def title = Some(name)

      def children = Seq(compilation, documentationWebsite, plugin, branching, webserver)

      def content = scalatex.documentation.Development()

      def details = Seq()

      def compilation = new DocumentationPage {
        def name = "Compilation"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.development.Compilation()
      }

      def documentationWebsite = new DocumentationPage {
        def name = "Documentation"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.development.DocumentationWebsite()
      }

      def plugin = new DocumentationPage {
        def name = "Plugins"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.development.Plugin()
      }

      def branching = new DocumentationPage {
        def name = "Branching model"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.development.Branching()
      }

      def webserver = new DocumentationPage {
        def name = "Web Server"

        override def title = Some(name)

        def children = Seq()

        def details = Seq()

        def content = scalatex.documentation.development.WebServer()
      }
    }
  }
}