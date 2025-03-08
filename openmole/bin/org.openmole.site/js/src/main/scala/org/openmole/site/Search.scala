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

import org.scalajs.dom.KeyboardEvent
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.lunr.IIndexSearchResult
import org.scalajs.dom
import com.raquo.laminar.api.L._

object Search {


  def build = {

    val centerSearch = Seq(
      width := "150",
      margin := "0 auto",
      display := "block"
    )
    val searchInput = inputTag("").amend(placeholder := "Search", centerSearch)

    val result: Var[Seq[IIndexSearchResult]] = Var(Seq())

    case class Item(index: Int = 0, ref: String = "", maxSize: Int = 0)

    val item = Var(Item())

    def search = () => {
      val oo = SiteJS.search(s"*${searchInput.ref.value}*")
      result.set(oo)
      item.set(Item())
      false
    }

    val searchOpen: Var[Boolean] = Var(false)

    val resultDiv = {
      val results = div(
        onKeyDown --> { (k: KeyboardEvent) =>
          val curItem = item.now()
          if (k.keyCode == 40 && curItem.index < result.now().size - 1) {
            item.set(curItem.copy(index = curItem.index + 1))
          }
          else if (k.keyCode == 38 && curItem.index > 0) {
            item.set(curItem.copy(index = curItem.index - 1))
          }
        },
        searchInput,
        child <-- result.signal.combineWith(item.signal).map { (rs, it) =>
          val rr = rs.take(10).zipWithIndex
          div(
            for {
              r ← rr
            } yield {
              div(paddingTop := "5", fontSize := "18px",
                a(cursor.pointer, href := r._1.ref, SiteJS.entries.get(r._1.ref), color := "white", {
                  if (r._2 == it.index) {
                    //item.set(it.copy(ref = r._1.ref, maxSize = rr.size))
                    backgroundColor := "rgb(164,60,60)"
                  }
                  else emptyMod
                }
                )
              )
            }
          )
        }
      )

      div(cls := "searchInput",
        child <-- searchOpen.signal.map { so =>
          if (so) {
            div(
              form(
                results,
                onKeyUp --> {
                  (k: KeyboardEvent) =>
                    if (k.keyCode != 38 && k.keyCode != 40)
                      search()
                },
                onSubmit.preventDefault --> { _ =>
                  if (item.now().ref != "")
                    org.scalajs.dom.window.location.href = item.now().ref
                }
              )
            )
          }
          else emptyNode
        }
      )
    }


    val searchDiv = org.scalajs.dom.window.document.getElementById(shared.searchDiv)
    val ddd = org.scalajs.dom.window.document.getElementById(shared.searchImg)

    ddd.addEventListener("mouseover", {
      (e: dom.MouseEvent) =>
        SiteJS.getIndex
    })

    ddd.addEventListener("click", {
      (e: dom.MouseEvent) =>
        searchOpen.update(!_)
        searchInput.ref.focus()
    })

    render(searchDiv, searchOpen.signal.expand(resultDiv))


  }
}
