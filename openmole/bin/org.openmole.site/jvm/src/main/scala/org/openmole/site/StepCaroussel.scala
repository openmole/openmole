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

case class Step(name: String, element: TypedTag[_ <: String], page: DocumentationPage)

class StepCarousel(current: Int, steps: Step*) {

  val currentStep = steps(current)
  val stepsSize = steps.size

  def toRight = steps((current + 1) % stepsSize).page

  def toLeft = steps((current + stepsSize - 1) % stepsSize).page

  val render = {
    div(width := "100%")(
      glyphSpan(glyph_chevron_left, previousDoc, toLeft),
      glyphSpan(glyph_chevron_right, nextDoc, toRight),
      div(stepHeader)(currentStep.name),
      currentStep.page.intro.map { i ⇒
        div(
          div(paddingTop := 50, i.intro),
          i.more.map { more ⇒
            div(more.render, id := shared.moreCollapse)
          }.getOrElse(div)
        )
      }.getOrElse(div),
      div(paddingTop := 50)(currentStep.element)
    )
  }
}