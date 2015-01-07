
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

package documentation

import scala.reflect.ClassTag
import scalatags.Text.all._
import scala.reflect.runtime.universe._

object Page {

  def menuEntry(p: Page) = a(p.name, href := p.file )

  def pageLine(p: Page): Frag =
    if (p.children.isEmpty) li(menuEntry(p))
    else li(menuEntry(p), ul(p.children.map(pageLine)))


  def menu(p: Page): Frag =
    Seq[Frag](
      menuEntry(p),
      p.children.map(pageLine)
    )

  def bottomLinks(p: Page) = {
    def previous(p: Page): Option[Page] =
      p.parent match {
        case None => None
        case Some(parent) =>
          parent.children.indexOf(p) match {
            case x if (x - 1) < 0 => None
            case x => Some(parent.children(x - 1))
          }
      }

    def next(p: Page): Option[Page] =
      p.parent match {
        case None => None
        case Some(parent) =>
          parent.children.indexOf(p) match {
            case x if (x + 1 >= parent.children.size) || (x == -1) => None
            case x => Some(parent.children(x + 1))
          }
      }

    def up(p: Page): Option[Page] = p.parent

    table (
      Seq("previous" -> previous(p), "up" -> up(p), "next" -> next(p)).map {
        case (_, None) => td()
        case (item, Some(p)) => td(a(item, href := p.file))
      }
    )
  }

  def decorate(p: Page) =
    table(
      td(verticalAlign:="top")(Page.menu(Pages)),
      td(verticalAlign:="top")(p.content, bottomLinks(p))
    )

  case class Parent(page: Option[Page])

}




abstract class Page(implicit p: Page.Parent = Page.Parent(None)) {

  def parent = p.page
  implicit def thisIsParent = Page.Parent(Some(this))

  def content: Frag
  def name: String
  def children: Seq[Page]

  def apply() = content

  def location: String =
    parent match {
      case None => name
      case Some(p) => p.location + "_" + name
    }

  def file = location + ".html"

  def allPages: Seq[Page] = {
    def pages(p: Page): List[Page] =
      p.children.toList ::: p.children.flatMap(_.allPages).toList
    this :: pages(this)
  }

  override def equals(o: scala.Any): Boolean =
   o match {
     case p2: Page => this.location == p2.location
     case _ => false
   }

  override def hashCode(): Int = location.hashCode()
}

object Pages extends Page() { index =>
  def children = Seq(console)
  def name = "documentation"

  def content = Index()

  def console =
    new Page {
      def name = "console"
      def children = Seq(task, sampling, transition, hook, environment)

      def content = documentation.console.Console()
      def task = new Page {
        def name = "task"
        def children = Seq(scala, systemExec, netLogo, mole)
        def content = documentation.console.Task()

        def scala = new Page {
          def name = "scala"
          def children = Seq()
          def content = documentation.console.task.Scala()
        }

        def systemExec = new Page {
          def name = "systemexec"
          def children = Seq()
          def content = documentation.console.task.SystemExec()
        }

        def netLogo = new Page {
          def name = "netlogo"
          def children = Seq()
          def content = documentation.console.task.NetLogo()
        }

        def mole = new Page {
          def name = "mole"
          def children = Seq()
          def content = documentation.console.task.MoleTask()
        }
      }

      def sampling = new Page {
        def name = "sampling"
        def children = Seq()
        def content = documentation.console.Sampling()
      }

      def transition = new Page {
        def name = "transition"
        def children = Seq()
        def content = documentation.console.Transition()
      }

      def hook = new Page {
        def name = "hook"
        def children = Seq()
        def content = documentation.console.Hook()
      }

      def environment = new Page {
        def name = "environment"
        def children = Seq(multithread, ssh, egi, cluster)
        def content = documentation.console.Environment()

        def multithread = new Page {
          def name = "multi-thread"
          def children = Seq()
          def content = documentation.console.environment.Multithread()
        }

        def ssh = new Page {
          def name = "SSH"
          def children = Seq()
          def content = documentation.console.environment.SSH()
        }

        def egi = new Page {
          def name = "EGI"
          def children = Seq()
          def content = documentation.console.environment.EGI()
        }

        def cluster = new Page {
          def name = "cluster"
          def children = Seq()
          def content = documentation.console.environment.Cluster()
        }

      }
    }

}
