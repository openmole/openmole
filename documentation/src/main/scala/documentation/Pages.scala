
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

import scalatags.Text.all.Frag

object Pages {

  case class Section(name: String)(pages: Seq[Page])
  case class Page(file: String, content: Frag)(sections: Seq[Section]*)


  /*Page("index.html", Index()) (
    Section("console") ( Page("task"))
  )
    def content = Index()
    def sections = Seq(
      new Section {
        def name = "console"
        def pages =Seq(new Page {

        })
      }
  }*/

  trait Dir {
    def dir: String
    def /(file: String) =
      (if(!dir.isEmpty) dir + "/" else "")  + file
  }

  object console extends Dir {
    def dir = ""
    def index = "console.html"
    def task = /("task.html")
    def java = /("java.html")
    def systemExec = /("systemexec.html")
    def netlogo = /("netlogo.html")
    def sampling = /("sampling.html")
  }
}
