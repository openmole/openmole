package org.openmole.site

import scalatags.Text.tags2
import scalatags.Text.all._
import org.openmole.core.buildinfo._

/*
 * Copyright (C) 22/06/17 // mathieu.leclaire@openmole.org
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

object Menu {

  val navClass = {
    import tools._
    classIs(navbar, navbar_default, navbar_staticTop, navbar_fixedTop, navbar_inverse)
  }
  val liStyle = paddingTop := 0
  val inputStyle = paddingTop := 15
  val navId = "omwesite"

  def bannerSpace = div(paddingTop := 130)

  //  def banner(sitePage: SitePage) = match {
  //    case cp: ContentPage => div(
  //      marginTop := 10,
  //      textAlign := "center",
  //      display := "block",
  //      marginLeft := "auto",
  //      marginRight := "auto",
  //      zIndex := 2000,
  //      `class` := "banner")(
  //        a(
  //          img(src := "img/banner/banner.png", width := "450px"),
  //          href := "http://exmodelo.org", target := "_blank"
  //        )
  //      )
  //    case _ => div
  //  }

  import scalatags.generic._
  def build(sitePage: SitePage) = {
    div(cls := "container")(
      bannerSpace,
      tags2.nav(navClass, paddingRight := 20)(
        div(cls := tools.container_fluid)(
          div(cls := tools.navbar_header)(
            button(`type` := "button", `class` := "navbar-toggle", data("toggle") := "collapse", data("target") := s"#$navId", marginTop := 25)(
              span(cls := "icon-bar"),
              span(cls := "icon-bar"),
              span(cls := "icon-bar")
            ),
            a(cls := tools.navbar_brand, href := "#", padding := 0),
            tools.to("")(
              img(alt := "", src := Resource.img.mole.openmoleLogo.file, Seq(width := 300, padding := 10, tools.pointer))
            )
          ),
          div(tools.classIs(tools.collapse, tools.navbar_collapse), aria.expanded := false, paddingTop := 20, id := navId)(
            ul(tools.classIs(tools.nav, tools.navbar_nav, tools.navbar_left), marginLeft := 200)(
              li(tools.innerLink(DocumentationPages.documentation, "Documentation"), liStyle),
              li(tools.innerLink(DocumentationPages.tutorials, "Tutorials"), liStyle),
              li(tools.outerLink("Demo", shared.link.demo), liStyle),
              li(tools.innerLink(DocumentationPages.download, "Download"), liStyle),
              li(tools.innerLink(DocumentationPages.OMcommunity, "Community"), liStyle),
              li(marginTop := -8, inputStyle)(img(id := shared.searchImg, src := Resource.img.menu.search.file, Seq(width := 35, paddingTop := 5, paddingLeft := 10, tools.pointer)))
            )
          ),
          div(id := shared.searchDiv)
        )
      )
    )
  }
}
