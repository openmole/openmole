package org.openmole.site

import scala.scalajs.js.annotation._
import scaladget.lunr.{IIndexSearchResult, Importedjs, Index}
import scala.scalajs.js.Dynamic.{literal => lit}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalajs.dom.document

/*
 * Copyright (C) 09/05/17 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


//@JSExportAll
@JSExportTopLevel(name = "openmole_site")
@JSExportAll
object SiteJS {


  @js.native
  trait IndexEntry extends js.Object {
    val title: String = js.native
    val url: String = js.native
  }

  type Entries = collection.mutable.Map[String, String]

  val indexArray: Var[js.Array[js.Any]] = Var(js.Array())
  val lunrIndex: Var[Option[Index]] = Var(None)
  var entries: Entries = collection.mutable.Map.empty

  def loadIndex(array: js.Array[js.Any]): Unit = {
    indexArray.set(array)
    Search.build
  }

  def doIndex(indexArray: js.Array[js.Any]) = {
    val index = Importedjs.lunr((i: Index) => {
      i.field("title", lit("boost" → 15).value)
      i.field("h2", lit("boost" -> 10).value)
      i.field("h3", lit("boost" -> 8).value)
      i.field("pre", lit("boost" -> 5).value) // for code tags
      i.field("body", lit("boost" → 1).value)
      i.ref("url")
      indexArray.foreach(p => {
        i.add(p)
        val ie = p.asInstanceOf[IndexEntry]
        entries.update(ie.url, ie.title)
      })
    })

    lunrIndex.set(Some(index))
  }

  def getIndex: Option[Index] =
  //Load index cache
    lunrIndex.now() match {
      case None => indexArray.now().size match {
        case 0 => None
        case _ =>
          doIndex(indexArray.now())
          getIndex
      }
      case i: Option[Index] => i
    }

  def search(content: String): Seq[IIndexSearchResult] = {
    getIndex.map {
      ind => ind.search(content).toSeq
    }.getOrElse(Seq())
  }

  def profileAnimation(): Unit = SVGStarter.decorateTrigger(shared.profile.button, shared.profile.animation, 11000)

  def pseAnimation(): Unit = SVGStarter.decorateTrigger(shared.pse.button, shared.pse.animation, 11000)

  def sensitivityAnimation(): Unit = SVGStarter.decorateTrigger(shared.sensitivity.button, shared.sensitivity.animation, 8000)
}