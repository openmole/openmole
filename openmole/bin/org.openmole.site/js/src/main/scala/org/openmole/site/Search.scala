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
import org.scalajs.dom.raw.MouseEvent
import sheet._
import rx._
import scaladget.api.Selector.Dropdown
import scaladget.mapping.lunr.IIndexSearchResult

object Search {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def build = {

    val centerSearch = Seq(
      width := 150,
      margin := "0 auto",
      display := "block"
    )

    val searchInput = bs.input("")(placeholder := "Search", centerSearch).render
    val result: Var[Seq[IIndexSearchResult]] = Var(Seq())

    val resultStyle: ModifierSeq = Seq(
      color := "black",
      left := -160,
      width := 200
    )

    def search = () ⇒ {
      result() = SiteJS.search(searchInput.value)
      false
    }

    val resultDiv = div(
      form(
        div(
          searchInput,
          Rx {
            div(scalatags.JsDom.all.paddingTop := 20)(
              for {
                r ← result().take(10)
              } yield {
                div(a(pointer, href := r.ref)(SiteJS.entries.get(r.ref)))
              }
            )
          }
        ),
        onkeyup := search
      )
    )

    val dd = new Dropdown(resultDiv, div, emptyMod, resultStyle, () ⇒ {})

    org.scalajs.dom.window.document.getElementById(shared.searchImg).addEventListener("click", {
      (e: MouseEvent) ⇒
        dd.toggle
        searchInput.focus()
    })

    org.scalajs.dom.window.document.getElementById(shared.searchDiv).appendChild(dd.render)

  }
}