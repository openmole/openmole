package org.openmole.site

import scaladget.bootstrapnative.bsn._
import scaladget.tools.JsRxTags._
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation._
import scalatags.JsDom.all._
import scalatags.JsDom.svgAttrs
import rx._

import scaladget.lunr.{ IIndexSearchResult, Importedjs, Index }
import scala.scalajs.js.Dynamic.{ literal ⇒ lit }
import scala.scalajs.js

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

@JSExportTopLevel("site.SiteJS")
object SiteJS extends JSApp {

  @JSExport()
  def main(): Unit = {
    Highlighting.init
    div.render
  }

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  @js.native
  trait IndexEntry extends js.Object {
    val title: String = js.native
    val url: String = js.native
  }

  type Entries = collection.mutable.Map[String, String]

  val lunrIndex: Var[Option[Index]] = Var(None)
  var entries: Entries = collection.mutable.Map.empty

  @JSExport
  def loadIndex(indexArray: js.Array[js.Any]): Unit = {
    Search.build

    val index = Importedjs.lunr((i: Index) ⇒ {
      i.field("title", lit("boost" → 15).value)
      i.field("h2", lit("boost" -> 10).value)
      i.field("h3", lit("boost" -> 8).value)
      i.field("pre", lit("boost" -> 5).value) // for code tags
      i.field("body", lit("boost" → 1).value)
      i.ref("url")
      indexArray.foreach(p ⇒ {
        i.add(p)
        val ie = p.asInstanceOf[IndexEntry]
        entries.update(ie.url, ie.title)
      })
    })

    lunrIndex() = Some(index)
  }

  def search(content: String): Seq[IIndexSearchResult] = {
    lunrIndex.now.map { i ⇒
      i.search(content).toSeq
    }.getOrElse(Seq())
  }

  @JSExport
  def loadBlogPosts(): Unit = {
    BlogPosts.fetch

    Rx {
      BlogPosts.all()
    }
  }

  @JSExport
  def profileAnimation(): Unit = SVGStarter.decorateTrigger(shared.profile.button, shared.profile.animation, 11000)

  @JSExport
  def pseAnimation(): Unit = SVGStarter.decorateTrigger(shared.pse.button, shared.pse.animation, 11000)

  @JSExport
  def sensitivityAnimation(): Unit = SVGStarter.decorateTrigger(shared.sensitivity.button, shared.sensitivity.animation, 8000)

  val visibleBanner = Var(true)

  // BANNER
  @JSExport
  def banner(): Unit = org.scalajs.dom.document.body.insertBefore(div(
    width := "100%",
    Rx {
      if (visibleBanner()) {
        div(
          marginTop := 10,
          width := "350px",
          display := "block",
          marginLeft := "auto",
          marginRight := "auto",
          zIndex := 2000,
          position := "relative",
          `class` := "banner")(
            a(
              img(src := "img/banner/banner.png", width := "350px"),
              href := "http://exmodelo.org", target := "_blank"
            )
          )
      }
      else div
    }
  ), org.scalajs.dom.document.body.firstChild
  )
}