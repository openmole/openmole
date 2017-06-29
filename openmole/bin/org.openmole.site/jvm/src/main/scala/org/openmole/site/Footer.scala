package org.openmole.site

import scalatags.Text.tags2
import scalatags.Text.all._
import org.openmole.site.tools._

/*
 * Copyright (C) 28/06/17 // mathieu.leclaire@openmole.org
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

object Footer {

  val navClass = classIs(navbar_default ++ navbar_inverse)
  val WHITE = color := "white"
  val BLUE = color := "#3086b5"

  val titleStyle = Seq(
    fontSize := "25px"
  )

  val textStyle = Seq(
    fontSize := "12px"
  )

  def subItem(image: RenameFileResource, title: String, link: String, text: String) =
    div(width := 200)(
      tools.to(link)(
        img(src := image.file, height := 25, paddingBottom := 5)(s" $title", titleStyle)
      ),
      div(text, textStyle)
    )

  val build = {
    tags2.nav(navClass, stylesheet.footer)(
      div(classIs(container_fluid), stylesheet.center)(
        div(classIs(collapse ++ navbar_collapse))(
          ul(classIs(nav ++ navbar_nav))(
            li(span("COMMUNITY", WHITE)(
              div(paddingTop := 15),
              subItem(Resource.img.github, "Source Repository", shared.link.github, "Follow the commits, submit an issue or take part to the devloppement !")
            ), padding := 15),
            li(div(href := Pages.faq.file, pointer, BLUE)("FAQ"), padding := 15)
          )
        )
      )
    )
  }

}
