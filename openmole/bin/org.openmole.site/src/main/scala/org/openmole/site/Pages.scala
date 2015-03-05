
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

import scalatags.Text.all
import scalatags.Text.all._

object Pages {

  def decorate(p: Page): Frag =
    p match {
      case p: DocumentationPage ⇒ DocumentationPages.decorate(p)
      case _                    ⇒ decorate(p.content)
    }

  def decorate(p: Frag): Frag =
    div(`class` := "container")(
      div(`class` := "header pull-center")(
        div(`class` := "title")(
          a(img(id := "logo", src := Resource.logo.file), href := index.file),
          a(img(id := "logo-version", src := Resource.versionLogo.file), href := Pages.gettingStarted.file)
        ),
        ul(id := "sections", `class` := "nav nav-pills")(
          li(a("Getting Started", `class` := "amenu", id := "section", href := gettingStarted.file)),
          li(a("Documentation", `class` := "amenu", id := "section", href := DocumentationPages.root.file)),
          li(a("Who are we?", `class` := "amenu", id := "section", href := whoAreWe.file))
        )
      ),
      div(`class` := "row")(p)
    )

  def index = Page("index", Index())
  def gettingStarted = Page("getting_started", GettingStarted())
  def whoAreWe = Page("who_are_we", WhoAreWe())
  def communications = Page("communications", Communications())

  def all: Seq[Page] = DocumentationPages.allPages ++ Seq(index, gettingStarted, whoAreWe, communications)

}

object Page {
  def apply(_name: String, _content: Frag) =
    new Page {
      override def name: String = _name
      override def content: all.Frag = _content
    }
}

trait Page {
  def content: Frag
  def name: String

  def location = name
  def file = location + ".html"
}

case class Parent[T](parent: Option[T])

abstract class DocumentationPage(implicit p: Parent[DocumentationPage] = Parent(None)) extends Page {
  def parent = p.parent
  implicit def thisIsParent = Parent[DocumentationPage](Some(this))

  def content: Frag
  def name: String
  def children: Seq[DocumentationPage]

  def apply() = content

  override def location: String =
    parent match {
      case None    ⇒ name
      case Some(p) ⇒ p.location + "_" + name
    }

  def allPages: Seq[DocumentationPage] = {
    def pages(p: DocumentationPage): List[DocumentationPage] =
      p.children.toList ::: p.children.flatMap(_.allPages).toList
    this :: pages(this)
  }

  override def equals(o: scala.Any): Boolean =
    o match {
      case p2: DocumentationPage ⇒ this.location == p2.location
      case _                     ⇒ false
    }

  override def hashCode(): Int = location.hashCode()
}

object DocumentationPages { index ⇒

  def decorate(p: DocumentationPage): Frag =
    Pages.decorate(
      Seq(
        div(id := "documentation-content", `class` := "row")(
          div(`class` := "col-sm-3")(documentationMenu(root, p)),
          div(`class` := "col-sm-9 main")(div(p.content, if (p != root) bottomLinks(p) else ""))
        )
      )
    )

  def documentationMenu(root: DocumentationPage, currentPage: DocumentationPage): Frag = {
    def menuEntry(p: DocumentationPage) = {
      def current = p == currentPage
      def idLabel = "documentation-menu-entry" + (if (current) "-current" else "")
      a(id := idLabel)(p.name, href := p.file)
    }

    def parents(p: DocumentationPage): List[DocumentationPage] =
      p.parent match {
        case None         ⇒ Nil
        case Some(parent) ⇒ parent :: parents(parent)
      }

    val currentPageParents = parents(currentPage).toSet

    def pageLine(p: DocumentationPage): Frag = {
      def contracted = li(menuEntry(p))
      def expanded = li(menuEntry(p), ul(id := "documentation-menu-ul")(p.children.map(pageLine)))

      if (p.children.isEmpty) contracted
      else if (p == currentPage) expanded
      else if (currentPageParents.contains(p)) expanded
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
      Seq("previous" -> previous(p), "up" -> up(p), "next" -> next(p)).map {
        case (t, None)       ⇒ td(id := "documentation-bottom-link-unavailable")(t)
        case (item, Some(p)) ⇒ td(id := "documentation-bottom-link")(a(item, href := p.file))
      }
    )
  }

  def allPages = root.allPages

  def root = new DocumentationPage {
    def name = "documentation"
    def content = documentation.Documentation()
    def children = Seq(console, gui, development)

    def console =
      new DocumentationPage {
        def name = "console"
        def children = Seq(task, sampling, transition, hook, environment, source, method, tutorial)
        def content = documentation.Console()

        def task = new DocumentationPage {
          def name = "task"
          def children = Seq(scala, systemExec, netLogo, mole)
          def content = documentation.console.Task()

          def scala = new DocumentationPage {
            def name = "scala"
            def children = Seq()
            def content = documentation.console.task.Scala()
          }

          def systemExec = new DocumentationPage {
            def name = "systemexec"
            def children = Seq()
            def content = documentation.console.task.SystemExec()
          }

          def netLogo = new DocumentationPage {
            def name = "netlogo"
            def children = Seq()
            def content = documentation.console.task.NetLogo()
          }

          def mole = new DocumentationPage {
            def name = "mole"
            def children = Seq()
            def content = documentation.console.task.MoleTask()
          }
        }

        def sampling = new DocumentationPage {
          def name = "sampling"
          def children = Seq()
          def content = documentation.console.Sampling()
        }

        def transition = new DocumentationPage {
          def name = "transition"
          def children = Seq()
          def content = documentation.console.Transition()
        }

        def hook = new DocumentationPage {
          def name = "hook"
          def children = Seq()
          def content = documentation.console.Hook()
        }

        def environment = new DocumentationPage {
          def name = "environment"
          def children = Seq(multithread, ssh, egi, cluster, desktopGrid)
          def content = documentation.console.Environment()

          def multithread = new DocumentationPage {
            def name = "multi-thread"
            def children = Seq()
            def content = documentation.console.environment.Multithread()
          }

          def ssh = new DocumentationPage {
            def name = "SSH"
            def children = Seq()
            def content = documentation.console.environment.SSH()
          }

          def egi = new DocumentationPage {
            def name = "EGI"
            def children = Seq()
            def content = documentation.console.environment.EGI()
          }

          def cluster = new DocumentationPage {
            def name = "cluster"
            def children = Seq()
            def content = documentation.console.environment.Cluster()
          }

          def desktopGrid = new DocumentationPage {
            def name = "desktop grid"
            def children = Seq()
            def content = documentation.console.environment.DesktopGrid()
          }

        }

        def source = new DocumentationPage {
          def name = "source"
          def children = Seq()
          def content = documentation.console.Source()
        }

        def method = new DocumentationPage {
          def name = "method"
          def children = Seq()
          def content = documentation.console.Method()
        }

        def tutorial = new DocumentationPage {
          def name = "tutorial"
          def children = Seq(helloWorld, headlessNetLogo, netLogoGA, capsule)
          def content = documentation.console.Tutorial()

          def helloWorld = new DocumentationPage {
            def name = "Hello World"
            def children = Seq()
            def content = documentation.console.tutorial.HelloWorld()
          }

          def headlessNetLogo = new DocumentationPage {
            def name = "NetLogo Headless"
            def children = Seq()
            def content = documentation.console.tutorial.HeadlessNetLogo()
          }

          def netLogoGA = new DocumentationPage {
            def name = "NetLogo GA"
            def children = Seq()
            def content = documentation.console.tutorial.NetLogoGA()
          }

          def capsule = new DocumentationPage {
            def name = "Capsule"
            def children = Seq()
            def content = documentation.console.tutorial.Capsule()
          }
        }
      }

    def gui = new DocumentationPage {
      def name = "GUI"
      def children = Seq()
      def content = documentation.GUI()
    }

    def development = new DocumentationPage {
      def name = "development"
      def children = Seq(compilation, plugin, branching, webserver)
      def content = documentation.Development()

      def compilation = new DocumentationPage {
        def name = "compilation"
        def children = Seq()
        def content = documentation.development.Compilation()
      }

      def plugin = new DocumentationPage {
        def name = "plugin"
        def children = Seq()
        def content = documentation.development.Plugin()
      }

      def branching = new DocumentationPage {
        def name = "branching"
        def children = Seq()
        def content = documentation.development.Branching()
      }

      def webserver = new DocumentationPage {
        def name = "web server"
        def children = Seq()
        def content = documentation.development.WebServer()
      }
    }
  }
}
