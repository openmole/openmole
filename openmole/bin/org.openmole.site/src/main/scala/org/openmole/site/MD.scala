/**
 * Created by Romain Reuillon on 05/07/16.
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
 *
 */
package org.openmole.site

import java.lang.StringBuilder
import java.util

import com.github.rjeschke._
import com.github.rjeschke.txtmark.DefaultDecorator
import org.openmole.site.market.GeneratedMarketEntry
import org.openmole.tool.file._

import scala.collection.JavaConversions._
import scalatags.Text.all._
import scalaz._

object MD {

  val emiter = new txtmark.BlockEmitter {
    override def emitBlock(stringBuilder: StringBuilder, list: util.List[String], s: String): Unit = {
      def code = list.mkString("\n")
      val html =
        if (s == "openmole") hl.openmole(code)
        else hl.highlight(code, s)
      stringBuilder.append(html.render)
    }
  }

  def apply(md: File): Frag = apply(md.content)

  def apply(md: String): Frag = {
    val configuration =
      txtmark.Configuration.builder().
        setCodeBlockEmitter(emiter).
        forceExtentedProfile().build()

    div(RawFrag(txtmark.Processor.process(md, configuration)))
  }

  def generatePage(entry: GeneratedMarketEntry)(implicit parent: Parent[DocumentationPage]) =
    entry.readme.map { md ⇒ DocumentationPages(entry.entry.name, Reader(_ ⇒ apply(md))) }

}
