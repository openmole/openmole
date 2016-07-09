package org.openmole.sitejs

/*
 * Copyright (C) 08/07/16 // mathieu.leclaire@openmole.org
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

import fr.iscpif.scaladget.mapping.lunr.{ IIndexSearchResult, Importedjs, Index }
import JsRxTags._
import scala.scalajs._
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import scalatags.JsDom.tags
import org.scalajs.dom
import rx._
import scala.scalajs.js.Dynamic.{ literal ⇒ lit }

@JSExport("Test")
object Test {
  val searchInput = input(placeholder := "Search").render
  val lunrIndex: Var[Option[Index]] = Var(None)
  val results: Var[Seq[IIndexSearchResult]] = Var(Seq())

  @JSExport
  def loadIndex(indexArray: js.Array[js.Any]): Unit = {
    val index = Importedjs.lunr((i: Index) ⇒ {
      i.field("title", lit("boost" → 10).value)
      i.field("body", lit("boost" → 1).value)
      i.ref("url")
      indexArray.foreach(p ⇒ {
        i.add(p)
      })
    })
    lunrIndex() = Some(index)
    val resultList = tags.div(
      Rx {
        for { r ← results() } yield {
          tags.div(
            tags.span(
              tags.a(r.ref, cursor := "pointer", href := r.ref, target := "_blank"),
              r.score
            )
          )
        }
      }
    ).render
    dom.document.getElementById("openmoleSearch").appendChild(
      tags.div(
      form(`type` := "submit", searchInput, textAlign := "center", onsubmit := { () ⇒
        search()
        false
      }), resultList
    ).render
    )
  }

  @JSExport
  def search(): Unit = {
    results() = lunrIndex.now.map { i ⇒
      i.search(searchInput.value).toSeq
    }.getOrElse(Seq())
  }

}
