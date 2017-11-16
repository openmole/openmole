package org.openmole.site

import scalatags.Text.{ TypedTag, tags2 }
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

  val WHITE = color := "#e6e6e6"

  val titleStyle = Seq(
    fontSize := "15px",
    marginLeft := 4
  )

  def liStyle(ratio: Int) = Seq(
    WHITE,
    padding := 20,
    width := s"$ratio%"
  )

  def subItem(i: TypedTag[String]) = div(paddingTop := 15)(i)

  def imgSubItem(image: FileResource, title: String, link: String, otherTab: Boolean = true) =
    subItem(
      div(width := "100%")(
        tools.to(link, otherTab = otherTab)(
          img(src := image.file, height := 20, paddingBottom := 5)(span(s"$title", titleStyle))
        )
      )
    )

  val build = {
    div(
      img(src := Resource.img.mole.openmole.file, stylesheet.leftMole),
      tags2.nav(classIs(navbar_default ++ navbar_inverse), stylesheet.footer)(
        div(classIs(container_fluid), stylesheet.center(50))(
          div(classIs(collapse ++ navbar_collapse))(
            ul(classIs(nav ++ navbar_nav))(
              li(liStyle(23))(
                span("COMMUNITY", textAlign := "center"),
                div(paddingTop := 15),
                imgSubItem(Resource.img.footer.email, "Forum", shared.link.mailingList),
                imgSubItem(Resource.img.footer.chat, "Chat", shared.link.chat),
                imgSubItem(Resource.img.footer.faq, "FAQ", Pages.faq.file, false)
              ),
              li(liStyle(26))(
                span("DEVELOPMENT", textAlign := "center"),
                div(paddingTop := 15),
                imgSubItem(Resource.img.footer.previousVersion, "Changes", Pages.previousVersions.file, false),
                imgSubItem(Resource.img.footer.github, "Source code", shared.link.repo.openmole),
                imgSubItem(Resource.img.footer.contribute, "Contribute!", DocumentationPages.howToContribute.file, false)
              ),
              li(liStyle(27))(
                span("ABOUT US", textAlign := "center"),
                div(paddingTop := 15),
                imgSubItem(Resource.img.footer.paper, "Publications", Pages.communications.file, true),
                imgSubItem(Resource.img.footer.whoarwe, "Who are we?", Pages.whoAreWe.file, false),
                imgSubItem(Resource.img.footer.partner, "Partners", Pages.partner.file, false)
              ),
              li(liStyle(22))(
                span("COMMUNICATION", textAlign := "center"),
                div(paddingTop := 15),
                imgSubItem(Resource.img.footer.blog, "Blog", shared.link.blog),
                imgSubItem(Resource.img.footer.twitter, "Twitter", shared.link.twitter)
              )
            )
          )
        )
      )
    )
  }

}
