package org.openmole.site

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

import tools._
import stylesheet._
import scalatags.Text.all._

object UserGuide {

  val firstModel = DocumentationPages.scala
  val firstMethod = DocumentationPages.calibration
  val firstEnvironment = DocumentationPages.multithread

  def headerModel(model: String) = span(
    tools.to(DocumentationPages.model)(img(src := Resource.img.codeAnimated.file, headerImg)),
    span(s"Run your own $model model", h1Like)
  )

  def headerMethod(method: String) = span(
    tools.to(DocumentationPages.method)(img(src := Resource.img.exploreMapAnimated.file, headerImg)),
    span(s"Explore with $method", h1Like)
  )

  def headerEnvironment(env: String) = span(
    tools.to(DocumentationPages.environment)(img(src := Resource.img.scaleAnimated.file, headerImg)),
    span(s"Scale on $env "), h1Like
  )

  lazy val imgStyle = Seq(
    width := 100,
    paddingRight := 15
  )

  def addCarousel(current: Page) = {

    val currentDetailMenu = LeftMenu.details(current.details)

    val currentStep = {
      if (DocumentationPages.modelPages.contains(current))
        Step(
          headerModel(current.name),
          div(current.content),
          LeftMenu.model.add(currentDetailMenu).build(300),
          firstModel, firstEnvironment, firstMethod
        )
      else if (DocumentationPages.methodPages.contains(current))
        Step(
          headerMethod(current.name),
          div(current.content),
          LeftMenu.method.add(currentDetailMenu).build(300),
          firstMethod, firstModel, firstEnvironment
        )
      else Step(
        headerEnvironment(current.name),
        div(current.content),
        LeftMenu.environment.add(currentDetailMenu).build(300),
        firstEnvironment, firstMethod, firstModel
      )
    }

    new StepCarousel(
      currentStep
    ).render

  }

}
