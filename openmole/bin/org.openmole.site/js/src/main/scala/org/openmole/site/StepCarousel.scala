package org.openmole.site

import org.openmole.site.sitesheet._
import org.scalajs.dom.raw.HTMLElement

import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import bs._

import sheet._

/*
 * Copyright (C) 14/04/17 // mathieu.leclaire@openmole.org
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
case class Step(name: String, element: HTMLElement, page: JSDocumentationPage)

class StepCarousel(current: Int, steps: Step*) {

  val introReplacer = utils.replacer
  val introMoreReplacer = utils.replacer

  val currentStep = steps(current)
  val stepsSize = steps.size

  def toRight = Search.to(steps((current + 1) % stepsSize).page)

  def toLeft = Search.to(steps((current + stepsSize - 1) % stepsSize).page)

  val render = {
    tags.div(width := "100%")(
      bs.glyphSpan(glyph_chevron_left +++ previousDoc, () ⇒ toLeft),
      bs.glyphSpan(glyph_chevron_right +++ nextDoc, () ⇒ toRight),
      tags.div(sitesheet.marginAuto +++ stepHeader)(currentStep.name),
      div(scalatags.JsDom.all.paddingTop := 50, introReplacer.tag),
      bs.button("More", btn_default).expandOnclick(introMoreReplacer.tag),
      div(scalatags.JsDom.all.paddingTop := 50)(currentStep.element)
    )
  }
}
