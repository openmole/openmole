package org.openmole.site

import scalatags.Text.tags2
import scalatags.Text.all._
import org.openmole.site.tools._
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

  val navClass = classIs(navbar ++ navbar_default ++ navbar_staticTop ++ navbar_fixedTop ++ navbar_inverse)
  val liStyle = paddingTop := 0
  val inputStyle = paddingTop := 15
  val navId = "omwesite"

  def build(sitePage: SitePage) = {
    div(classIs("container"))(
      tags2.nav(navClass, paddingRight := 20)(
        div(classIs(container_fluid))(
          div(classIs(navbar_header))(
            button(`type` := "button", `class` := "navbar-toggle", data("toggle") := "collapse", data("target") := s"#$navId", marginTop := 25)(
              span(classIs("icon-bar")),
              span(classIs("icon-bar")),
              span(classIs("icon-bar"))
            ),
            a(classIs(navbar_brand), href := "#", padding := 0),
            to(Pages.index)(
              img(alt := "", src := Resource.img.mole.openmoleText.file, Seq(width := 240, paddingTop := 20, pointer))
            )
          ),
          div(classIs(collapse ++ navbar_collapse), aria.expanded := false, paddingTop := 20, id := navId)(
            ul(classIs(nav ++ navbar_nav ++ navbar_right))(
              li(innerLink(DocumentationPages.documentation, "DOCUMENTATION"), liStyle),
              li(innerLink(DocumentationPages.tutorials, "TUTORIALS"), liStyle),
              li(innerLink(DocumentationPages.OMcommunity, "COMMUNITY"), liStyle),
              // li(marginTop := -8, marginBottom := 8, divLinkButton(div(maxWidth := 140)(span("DOWNLOAD"), span(version.value, fontSize := "10px", paddingLeft := 7)), Resource.script.openmole.file, classIs(btn ++ btn_primary))),
              li(innerLink(DocumentationPages.install, "DOWNLOAD"), liStyle),
              li(marginTop := -8, inputStyle)(img(id := shared.searchImg, src := Resource.img.menu.search.file, Seq(width := 35, paddingTop := 5, paddingLeft := 10, pointer)))(
                div(id := shared.searchDiv)
              )
            )
          )
        )
      )
    )
  }
}
