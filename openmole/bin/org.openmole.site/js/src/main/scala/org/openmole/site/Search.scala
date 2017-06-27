package org.openmole.site

/*
 * Copyright (C) 19/04/17 // mathieu.leclaire@openmole.org
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

import scaladget.stylesheet.{ all ⇒ sheet }
import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.tools.JsRxTags._
import scalatags.JsDom.all._
import sheet._
import rx._

import scaladget.api.Selector.Dropdown
import scaladget.mapping.lunr.IIndexSearchResult

object Search {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def build = {

    val searchDiv = div
    lazy val searchInput = bs.input("")(placeholder := "Search", width := 150).render
    val result: Var[Seq[IIndexSearchResult]] = Var(Seq())
    val resultDiv =
      div(
        Rx {
          for {
            r ← result().take(10)
          } yield {
            div(a(pointer, href := r.ref)(SiteJS.entries.get(r.ref)))
          }
        }
      )

    val resultStyle: ModifierSeq = Seq(
      color := "black",
      left := -20,
      width := 200
    )

    val dd = new Dropdown(resultDiv, div(searchDiv), emptyMod, resultStyle, () ⇒ {})

    val searchBlock = div(
      form(
      searchInput,
      onsubmit := { () ⇒
        result() = SiteJS.search(searchInput.value)
        dd.toggle
        false
      }
    ).render,
      Rx {
        if (result().size > 0) dd.render else div.render
      }
    ).render

    org.scalajs.dom.window.document.getElementById(shared.searchDiv).appendChild(searchBlock)

  }
}