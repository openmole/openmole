package org.openmole.site

import org.scalajs.dom.raw.HTMLElement

import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.tools.JsRxTags._
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation._
import scalatags.JsDom.tags
import scalatags.JsDom.all.{ input, _ }
import bs._
import rx._

import scaladget.mapping.lunr.{ IIndexSearchResult, Importedjs, Index }
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

    //    JSPages.toJSPage(org.scalajs.dom.window.location.pathname.split('/').last) foreach { page ⇒
    //
    //      if (JSPages.topPagesChildren.contains(page)) UserGuide.addCarousel(page)
    //      else MainPage.load(page)
    //
    //      Highlighting.init
    //    }
    withBootstrapNative {
      Expander()
      Highlighting.init
      div.render
    }

  }

  val lunrIndex: Var[Option[Index]] = Var(None)

  @JSExport
  def loadIndex(indexArray: js.Array[js.Any]): Unit = {
    Search.build

    val index = Importedjs.lunr((i: Index) ⇒ {
      i.field("title", lit("boost" → 10).value)
      i.field("body", lit("boost" → 1).value)
      i.ref("url")
      indexArray.foreach(p ⇒ {
        i.add(p)
      })
    })

    lunrIndex() = Some(index)
  }

  def search(content: String): Seq[IIndexSearchResult] = {
    lunrIndex.now.map { i ⇒
      i.search(content).toSeq
    }.getOrElse(Seq())
  }

}