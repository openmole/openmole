package org.openmole.site

import scalatags.Text.tags2
import scalatags.Text.all._
import org.openmole.site.tools._

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
  val liStyle = paddingTop := 8
  val inputStyle = paddingTop := 15

  val build = {
    tags2.nav(navClass, paddingRight := 20)(
      div(classIs(container_fluid))(
        div(classIs(navbar_header))(
          div(classIs(navbar_brand), href := "#", padding := 0),
          to(Pages.index)(
            img(alt := "", src := "img/openmole.png", Seq(width := 240, pointer))
          )
        ),
        div(classIs(collapse ++ navbar_collapse), paddingTop := 10)(
          ul(classIs(nav ++ navbar_nav ++ navbar_right))(
            li(innerLink(DocumentationPages.scala, "DOCUMENTATION"), liStyle),
            li(inputStyle)(div(id := shared.searchDiv)),
            li(linkButton("DEMO", shared.link.demo, classIs(btn ++ btn_primary)))
          )
        )
      )
    )
  }
}