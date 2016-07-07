
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

import scalaz.Reader

object Pages {

  def decorate(p: Page): Reader[File, Frag] =
    p match {
      case p: DocumentationPage ⇒ DocumentationPages.decorate(p)
      case _                    ⇒ p.content.map(decorate)
    }

  def decorate(p: Frag): Frag =
    div(`class` := "container")(
      div(`class` := "header pull-center")(
        div(`class` := "title")(a(img(id := "logo", src := Resource.logo.file), href := index.file)),
        ul(id := "sections", `class` := "nav nav-pills")(
          li(a("Getting Started", `class` := "amenu", id := "section", href := gettingStarted.file)),
          li(a("Documentation", `class` := "amenu", id := "section", href := DocumentationPages.root.file)),
          li(a("Who are we?", `class` := "amenu", id := "section", href := whoAreWe.file))
        )
      ),
      div(`class` := "row")(p)
    )

  def index = Page("index", scalatex.Index(), title = Some("OpenMOLE: scientific workflow, distributed computing, parameter tuning"))
  def gettingStarted = Page("getting_started", scalatex.GettingStarted(), title = Some("Getting started with OpenMOLE - introductory tutorial"))
  def whoAreWe = Page("who_are_we", scalatex.WhoAreWe(), title = Some("Developers, reference publications, contact information - OpenMOLE"))
  def communications = Page("communications", scalatex.Communications(), title = Some("Related papers, conference slides, videos, OpenMOLE in the news"))

  def all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, whoAreWe, communications)

}

object Page {
  def apply(name: String, content: Frag, title: Option[String] = None) = {
    val (_name, _content, _title) = (name, content, title)

    new Page {
      override def name: String = _name
      override def content = Reader(_ ⇒ _content)
      override def title = _title
    }
  }
}

trait Page {
  def content: Reader[File, Frag]
  def name: String
  def title: Option[String]

  def location = name
  def file = location + ".html"
}

case class Parent[T](parent: Option[T])

abstract class DocumentationPage(implicit p: Parent[DocumentationPage] = Parent(None)) extends Page {
  def parent = p.parent
  implicit def thisIsParent = Parent[DocumentationPage](Some(this))

  def content: Reader[File, Frag]
  def name: String
  def children: Seq[DocumentationPage]
  def title = None

  override def location: String =
    parent match {
      case None    ⇒ name
      case Some(p) ⇒ p.location + "_" + name
    }

  def allPages: Seq[DocumentationPage] = {
    def pages(p: DocumentationPage): List[DocumentationPage] =
      p.children.toList ::: p.children.flatMap(_.allPages).toList
    this :: pages(this)
  }.distinct

  override def equals(o: scala.Any): Boolean =
    o match {
      case p2: DocumentationPage ⇒ this.location == p2.location
      case _                     ⇒ false
    }

  override def hashCode(): Int = location.hashCode()
}

object DocumentationPages { index ⇒

  var marketEntries: Seq[GeneratedMarketEntry] = Seq()

  def apply(name: String, content: Reader[File, Frag], children: Seq[DocumentationPage] = Seq.empty)(implicit p: Parent[DocumentationPage] = Parent(None)) = {
    val (_name, _content, _children) = (name, content, children)
    new DocumentationPage {
      override def children = _children
      override def name = _name
      override def content = _content
    }
  }

  def tag(p: DocumentationPage): String = p.name + p.parent.map(pa ⇒ "-" + tag(pa)).getOrElse("")

  def decorate(p: DocumentationPage) =
    p.content.map { content ⇒
      Pages.decorate(
        Seq(
          div(id := "documentation-content", `class` := "row")(
            div(`class` := "col-sm-3")(documentationMenu(root, p)),
            div(`class` := "col-sm-9", id := "documentation-text")(div(content, if (p != root) bottomLinks(p) else ""))
          )
        )
      )
    }

  def documentationMenu(root: DocumentationPage, currentPage: DocumentationPage): Frag = {
    def menuEntry(p: DocumentationPage) = {
      def current = p.location == currentPage.location
      def idLabel = "documentation-menu-entry" + (if (current) "-current" else "")
      a(p.name, href := p.file)
    }

    def parents(p: DocumentationPage): List[DocumentationPage] =
      p.parent match {
        case None         ⇒ Nil
        case Some(parent) ⇒ parent :: parents(parent)
      }

    val currentPageParents = parents(currentPage).toSet

    def pageLine(p: DocumentationPage): Frag = {

      def contracted = li(menuEntry(p))
      def expanded =
        li(
          menuEntry(p),
          div(id := tag(p) + "-menu", ul(id := "documentation-menu-ul")(p.children.map(pageLine)))
        )

      if (p.children.isEmpty) contracted
      else if (p.location == currentPage.location) expanded
      else if (currentPageParents.exists(_.location == p.location)) expanded
      else contracted
    }

    div(id := "documentation-menu")(
      root.children.map(pageLine)
    )
  }

  def bottomLinks(p: DocumentationPage) = {
    def previous(p: DocumentationPage): Option[DocumentationPage] =
      p.parent match {
        case None ⇒ None
        case Some(parent) ⇒
          parent.children.indexOf(p) match {
            case x if (x - 1) < 0 ⇒ None
            case x                ⇒ Some(parent.children(x - 1))
          }
      }

    def next(p: DocumentationPage): Option[DocumentationPage] =
      p.parent match {
        case None ⇒ None
        case Some(parent) ⇒
          parent.children.indexOf(p) match {
            case x if (x + 1 >= parent.children.size) || (x == -1) ⇒ None
            case x ⇒ Some(parent.children(x + 1))
          }
      }

    def up(p: DocumentationPage): Option[DocumentationPage] = p.parent

    table(id := "documentation-bottom-links")(
      Seq("previous" → previous(p), "up" → up(p), "next" → next(p)).map {
        case (t, None)       ⇒ td(id := "documentation-bottom-link-unavailable")(t)
        case (item, Some(p)) ⇒ td(id := "documentation-bottom-link")(a(item, href := p.file))
      }
    )
  }

  def allPages = root.allPages

  def root = new DocumentationPage {
    def name = "Documentation"
    def content = Reader(_ ⇒ scalatex.documentation.Documentation())
    def children = Seq(application, language, tutorial, market, faq, development)

    def application = new DocumentationPage {
      def name = "Application"
      def children = Seq(migration)
      def content = Reader(_ ⇒ scalatex.documentation.Application())

      def migration = new DocumentationPage() {
        def children: Seq[DocumentationPage] = Seq()
        def name: String = "Migration"
        def content = Reader(_ ⇒ scalatex.documentation.application.Migration())
      }
    }

    def language =
      new DocumentationPage {
        def name = "Language"
        def children = Seq(task, sampling, transition, hook, environment, source, method)
        def content = Reader(_ ⇒ scalatex.documentation.Language())

        def task = new DocumentationPage {
          def name = "Tasks"
          def children = Seq(scala, native, netLogo, mole)
          def content = Reader(_ ⇒ scalatex.documentation.language.Task())

          def scala = new DocumentationPage {
            def name = "Scala"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.task.Scala())
          }

          def native = new DocumentationPage {
            def name = "Native"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.task.Native())
          }

          def netLogo = new DocumentationPage {
            def name = "NetLogo"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.task.NetLogo())
          }

          def mole = new DocumentationPage {
            def name = "Mole"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.task.MoleTask())
          }
        }

        def sampling = new DocumentationPage {
          def name = "Samplings"
          def children = Seq()
          def content = Reader(_ ⇒ scalatex.documentation.language.Sampling())
        }

        def transition = new DocumentationPage {
          def name = "Transitions"
          def children = Seq()
          def content = Reader(_ ⇒ scalatex.documentation.language.Transition())
        }

        def hook = new DocumentationPage {
          def name = "Hooks"
          def children = Seq()
          def content = Reader(_ ⇒ scalatex.documentation.language.Hook())
        }

        def environment = new DocumentationPage {
          def name = "Environments"
          def children = Seq(multithread, ssh, egi, cluster, desktopGrid)
          def content = Reader(_ ⇒ scalatex.documentation.language.Environment())

          def multithread = new DocumentationPage {
            def name = "Multi-threads"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.environment.Multithread())
          }

          def ssh = new DocumentationPage {
            def name = "SSH"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.environment.SSH())
          }

          def egi = new DocumentationPage {
            def name = "EGI"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.environment.EGI())
          }

          def cluster = new DocumentationPage {
            def name = "Clusters"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.environment.Cluster())
          }

          def desktopGrid = new DocumentationPage {
            def name = "Desktop Grid"
            def children = Seq()
            def content = Reader(_ ⇒ scalatex.documentation.language.environment.DesktopGrid())
          }

        }

        def source = new DocumentationPage {
          def name = "Sources"
          def children = Seq()
          def content = Reader(_ ⇒ scalatex.documentation.language.Source())
        }

        def method = new DocumentationPage {
          def name = "Methods"
          def children = Seq()
          def content = Reader(_ ⇒ scalatex.documentation.language.Method())
        }
      }

    def tutorial = new DocumentationPage {
      def name = "Tutorials"
      def children =
        Seq(helloWorld, headlessNetLogo, netLogoGA, capsule) ++
          marketEntries.filter(_.tags.exists(_ == Tags.tutorial)).flatMap(MD.generatePage(_))

      def content = Reader(_ ⇒ scalatex.documentation.language.Tutorial())

      def helloWorld = new DocumentationPage {
        def name = "Hello World"
        def children = Seq()
        def content = Pages.gettingStarted.content
      }

      def headlessNetLogo = new DocumentationPage {
        def name = "NetLogo Headless"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.language.tutorial.HeadlessNetLogo())
      }

      def netLogoGA = new DocumentationPage {
        def name = "GA with NetLogo"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.language.tutorial.NetLogoGA())
      }

      def capsule = new DocumentationPage {
        def name = "Capsule"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.language.tutorial.Capsule())
      }
    }

    def market = new DocumentationPage {
      def children: Seq[DocumentationPage] = pages
      def name: String = "Market Place"
      def content = Reader(_ ⇒ scalatex.documentation.Market())

      def themes: Seq[Market.Tag] =
        marketEntries.flatMap(_.entry.tags).distinct.sortBy(_.label.toLowerCase)

      def allEntries =
        new DocumentationPage {
          def children: Seq[DocumentationPage] = Seq()
          def name: String = "All"
          def content = Reader(_ ⇒ tagContent("All", marketEntries))
        }

      def pages = allEntries :: (themes map documentationPage).toList

      def documentationPage(t: Market.Tag) =
        new DocumentationPage {
          def children: Seq[DocumentationPage] = Seq()
          def name: String = t.label
          def content = Reader(_ ⇒ tagContent(t.label, marketEntries.filter(_.entry.tags.contains(t))))
        }

      def tagContent(label: String, entries: Seq[GeneratedMarketEntry]) =
        Seq(
          h1(label),
          ul(
            entries.sortBy(_.entry.name.toLowerCase).map {
              de ⇒ li(entryContent(de))
            }: _*
          )
        )

      def entryContent(deployedMarketEntry: GeneratedMarketEntry) = {
        Seq(
          a(deployedMarketEntry.entry.name, href := deployedMarketEntry.archive),
          p(
            div(id := "market-entry")(
              Seq(
                deployedMarketEntry.readme.map {
                  rm ⇒ RawFrag(txtmark.Processor.process(rm))
                }.getOrElse(p("No README.md available yet."))
              ) ++ deployedMarketEntry.viewURL.map { u ⇒ a("source repository", href := u) }: _*
            )
          )
        )
      }

    }

    def faq = new DocumentationPage {
      def name = "FAQ"
      def children = Seq()
      def content = Reader(_ ⇒ scalatex.documentation.FAQ())
    }

    def development = new DocumentationPage {
      def name = "Development"
      def children = Seq(compilation, documentationWebsite, plugin, branching, webserver)
      def content = Reader(_ ⇒ scalatex.documentation.Development())

      def compilation = new DocumentationPage {
        def name = "Compilation"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.development.Compilation())
      }

      def documentationWebsite = new DocumentationPage {
        def name = "Documentation"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.development.DocumentationWebsite())
      }

      def plugin = new DocumentationPage {
        def name = "Plugins"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.development.Plugin())
      }

      def branching = new DocumentationPage {
        def name = "Branching model"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.development.Branching())
      }

      def webserver = new DocumentationPage {
        def name = "Web Server"
        def children = Seq()
        def content = Reader(_ ⇒ scalatex.documentation.development.WebServer())
      }
    }
  }
}
