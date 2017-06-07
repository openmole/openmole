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
import scaladget.api.Popup._
import scaladget.tools.JsRxTags._
import scalatags.JsDom.all._
import sheet._
import bs._
import rx._

import scaladget.api.Selector.Dropdown
import scaladget.mapping.lunr.IIndexSearchResult

object Menu {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def to(page: JSPage) {
    org.scalajs.dom.window.location.href = page.file
  }

  def build = {
    val docItem = stringNavItem("DOCUMENTATION", () ⇒
      to(JSPages.documentation_language_models_java))

    val downloadItem = stringNavItem("DOWNLOAD", () ⇒
      println("DOWN")
    // currentCatergory() = category.Download
    )

    val faqItem = stringNavItem("FAQ", () ⇒
      to(JSPages.faq))

    val demoItem = navItem(
      bs.linkButton("DEMO", "http://demo.openmole.org", btn_primary).render
    )

    val searchItem = {

      val searchDiv = div()
      lazy val searchInput = bs.input("")(placeholder := "Search", width := 150).render
      val result: Var[Seq[IIndexSearchResult]] = Var(Seq())
      val resultDiv = div(Rx {
        for {
          r ← result().take(10)
        } yield {
          val page = JSPages.all.filter { p: JSPage ⇒ p.file == r.ref }.head
          div(a(pointer, onclick := { () ⇒ to(page) })(page.name))
        }
      })

      val dd = new Dropdown(resultDiv, searchDiv, emptyMod, sitesheet.searchResult, () ⇒ {})

      navItem(
        div(
        form(
        searchInput,
        onsubmit := { () ⇒
          result() = SiteJS.search(searchInput.value)
          dd.toggle
          false
        }
      ).render,
        dd.render,
        searchDiv
      ).render
      )
    }

    val issueItem = navItem(
      bs.linkButton("ISSUES", "http://discourse.iscpif.fr", btn_primary).render,
      extraRenderPair = navbar_right
    )

    //Create the nav bar
    bs.navBar(
      navbar_staticTop +++ navbar_fixedTop +++ navbar_inverse +++ Seq(scalatags.JsDom.all.paddingRight := 20, height := 70),
      docItem.right,
      faqItem.right,
      downloadItem.right,
      searchItem.right,
      demoItem.right,
      issueItem.right
    ).withBrand("img/openmole.png", width := 240, () ⇒ {
        to(JSPages.index)
        org.scalajs.dom.window.location.href = JSPages.index.file
      }).render
  }
}