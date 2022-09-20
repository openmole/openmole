//package org.openmole.site
//
///*
// * Copyright (C) 19/04/17 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//import org.scalajs.dom.KeyboardEvent
//import scaladget.bootstrapnative.bsn._
//import scaladget.tools._
//import scaladget.lunr.IIndexSearchResult
//import scalatags.JsDom.all._
//import org.scalajs.dom.raw.MouseEvent
//import rx._
//import scaladget.bootstrapnative.Selector.Dropdown
//
//object Search {
//
//  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
//
//  def build(getIndex: () ⇒ Unit) = {
//
//    val centerSearch = Seq(
//      width := 150,
//      margin := "0 auto",
//      display := "block"
//    )
//
//    val searchInput = inputTag("")(placeholder := "Search", centerSearch).render
//    val result: Var[Seq[IIndexSearchResult]] = Var(Seq())
//
//    case class Item(index: Int = 0, ref: String = "", maxSize: Int = 0)
//    val item = Var(Item())
//
//    val resultStyle: ModifierSeq = Seq(
//      color := "black",
//      left := -160,
//      width := 200
//    )
//
//    def search = () ⇒ {
//      result() = SiteJS.search(s"*${searchInput.value}*")
//      item() = Item()
//      false
//    }
//
//    val resultDiv = {
//      lazy val results = div(
//        onkeydown := { (k: KeyboardEvent) ⇒
//          val curItem = item.now
//          if (k.keyCode == 40 && curItem.index < curItem.maxSize - 1) {
//            item() = curItem.copy(index = curItem.index + 1)
//            false
//          }
//          else if (k.keyCode == 38 && curItem.index > 0) {
//            item() = curItem.copy(index = curItem.index - 1)
//            false
//          }
//        },
//        searchInput,
//        Rx {
//          val rr = result().take(10).zipWithIndex
//          div(scalatags.JsDom.all.paddingTop := 20)(
//            for {
//              r ← rr
//            } yield {
//              div(
//                a(pointer, href := r._1.ref)(SiteJS.entries.get(r._1.ref)), {
//                  if (r._2 == item().index) {
//                    item() = item.now.copy(ref = r._1.ref, maxSize = rr.size)
//                    backgroundColor := "#ddd"
//                  }
//                  else color := "black"
//                }
//              )
//            }
//          )
//        }
//      )
//
//      div(
//        form(
//          results,
//          onkeyup := {
//            (k: KeyboardEvent) ⇒
//              if (k.keyCode != 38 && k.keyCode != 40)
//                search()
//          },
//          onsubmit := { () ⇒
//            if (item.now.ref != "")
//              org.scalajs.dom.window.location.href = item.now.ref
//            false
//          }
//        )
//      )
//    }
//
//    val dd = new Dropdown(resultDiv, div, emptyMod, resultStyle, () ⇒ {})
//
//    val ddd = org.scalajs.dom.window.document.getElementById(shared.searchImg)
//    ddd.addEventListener("mouseover", {
//      (e: MouseEvent) ⇒
//        getIndex()
//    })
//
//    ddd.addEventListener("click", {
//      (e: MouseEvent) ⇒
//        dd.toggle
//        searchInput.focus()
//    })
//
//    org.scalajs.dom.window.document.getElementById(shared.searchDiv).appendChild(dd.render)
//
//  }
//}
