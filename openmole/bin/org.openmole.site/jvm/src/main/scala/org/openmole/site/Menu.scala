package org.openmole.site

import scalatags.Text.tags2
import scalatags.Text.all._

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

  def classIs(s: String) = `class` := s
  def styleIs(s: String) = `style` := s
  val targetBlank = target := "_blank"

  def to(ref: String) = a(href := ref)
  def innerLink(page: Page, title: String) = to(page.file)(span(title))
  def buttonLink(ref: String, buttonTitle: String) = to(ref)(targetBlank)(span(classIs("btn btn-primary"), `type` := "button", buttonTitle))

  val navClass = classIs("navbar navbar-default navbar-static-top navbar-fixed-top navbar-inverse")
  val liStyle = styleIs("padding-top: 8px;")

  def build() = {
    tags2.nav(navClass, styleIs("padding-right: 20px;"))(
      div(classIs("container-fluid"))(
        div(classIs("navbar-header"))(
          div(classIs("navbar-brand"), href := "#", styleIs("padding: 0px;")),
          to("index.html")(
            img(alt := "", src := "img/openmole.png", styleIs("width: 240px; cursor: pointer;"))
          )
        ),
        div(classIs("collapse navbar-collapse"), styleIs("padding-top:10px;"))(
          ul(classIs("nav navbar-nav navbar-right"))(
            li(innerLink(DocumentationPages.root.language.model.scala, "DOCUMENTATION"), liStyle),
            li(innerLink(Pages.faq, "FAQ"), liStyle),
            li(buttonLink("http://demo.openmole.org", "DEMO"))
          )
        )
      )
    )
  }
}