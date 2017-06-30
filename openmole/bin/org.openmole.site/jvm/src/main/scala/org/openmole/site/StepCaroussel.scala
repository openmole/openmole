package org.openmole.site

import scalatags.Text.TypedTag
import scalatags.Text.all._
import stylesheet._
import tools._

/*
 * Copyright (C) 23/06/17 // mathieu.leclaire@openmole.org
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

case class Step(name: TypedTag[_ <: String], element: TypedTag[_ <: String], page: DocumentationPage, previous: DocumentationPage, next: DocumentationPage)

class StepCarousel(step: Step) {

  val render = {
    div(width := "100%")(
      glyphSpan(glyph_chevron_left, previousDoc, step.previous),
      glyphSpan(glyph_chevron_right, nextDoc, step.next),
      div(maxHeight := 100)(
        div(stepHeader)(step.name),
        hr(classIs("line"), width := "80%", marginTop := 40)
      ),
      step.page.intro.map { i ⇒
        div(
          div(paddingTop := 20, i.intro),
          i.more.map { more ⇒
            div(more.render, id := shared.moreCollapse)
          }.getOrElse(div)
        )
      }.getOrElse(div),
      div(paddingTop := 50)(step.element)
    )
  }
}