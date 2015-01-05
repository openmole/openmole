
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

  private def pageLine(p: Page): Frag = {
    def ref = a(p.name, href := p.file )
    if (p.children.isEmpty) li(ref)
    else li(ref, ul(p.children.map(pageLine)))
  }

  def menu(p: Page): Frag =
    ul ( p.children.map(pageLine) )

}

abstract class Page(implicit parent: Option[Page] = None) {

  implicit def thisIsParent: Option[Page] = Some(this)

  def content: Frag
  def name: String
  def children: Seq[Page]

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

}

object Pages extends Page() { index =>
  def children = Seq(console)
  def name = "index"

  def content = Index()

  def console =
    new Page {
      def name = "console"
      def children = Seq(task, sampling, transition)

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

      def transition =  new Page {
        def name = "transition"
        def children = Seq()
        def content = documentation.console.Transition()
      }
    }

}
