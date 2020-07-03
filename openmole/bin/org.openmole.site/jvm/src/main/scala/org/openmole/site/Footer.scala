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

  val WHITE = color := "#ffffff"

  val titleStyle = Seq(
    marginLeft := 4
  )

  def liStyle(ratio: Int) = Seq(
    WHITE,
    padding := 20,
    width := s"$ratio%"
  )

  def subItem(i: TypedTag[String]) = div(paddingTop := 8)(i)

  def imgSubItem(image: FileResource, title: String, link: String, otherTab: Boolean = true) =
    subItem(
      div(width := "100%")(
        img(src := image.file, height := 20, paddingBottom := 5),
        tools.to(link, otherTab = otherTab, style = Seq(WHITE))(span(s"$title", titleStyle))
      )
    )

  val footerColStyle = Seq(
    paddingTop := 20,
    paddingLeft := 25,
    width := 160
  )

  val build = {
    div(
      //img(src := Resource.img.mole.openmole.file, stylesheet.leftMole),
      div(stylesheet.footer)(
        div(
          div(id := "footer")(
            div(classIs("inner-footer"))(
              div(classIs(s"$row"))(
                div(classIs(colMD(3)), footerColStyle)(
                  span("Community", textAlign := "center"),
                  imgSubItem(Resource.img.footer.email, "Forum", shared.link.mailingList),
                  imgSubItem(Resource.img.footer.chat, "Chat", shared.link.chat),
                  imgSubItem(Resource.img.footer.faq, "FAQ", DocumentationPages.faq.file, false)
                ),
                div(classIs(colMD(3)), footerColStyle)(
                  span("Development", textAlign := "center"),
                  imgSubItem(Resource.img.footer.previousVersion, "Changes", DocumentationPages.releaseNotes.file, false),
                  imgSubItem(Resource.img.footer.github, "Sources", shared.link.repo.openmole),
                  imgSubItem(Resource.img.footer.contribute, "Join us!", DocumentationPages.howToContribute.file, false)
                ),
                div(classIs(colMD(3)), footerColStyle)(
                  span("About us", textAlign := "center"),
                  imgSubItem(Resource.img.footer.paper, "Papers", DocumentationPages.communications.file, false),
                  imgSubItem(Resource.img.footer.whoarwe, "Team", DocumentationPages.whoWeAre.file, false),
                  imgSubItem(Resource.img.footer.partner, "Partners", DocumentationPages.partner.file, false)
                ),
                div(classIs(colMD(3)), footerColStyle)(
                  span("Communication", textAlign := "center"),
                  imgSubItem(Resource.img.footer.blog, "Blog", shared.link.blog),
                  imgSubItem(Resource.img.footer.twitter, "Twitter", shared.link.twitter)
                )
              )
            )
          )
        )
      )
    )
  }

}
