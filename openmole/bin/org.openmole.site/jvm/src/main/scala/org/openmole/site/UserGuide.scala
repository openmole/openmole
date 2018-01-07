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

import stylesheet._

import scalatags.Text.all._
import SideMenu._
import org.openmole.site.tools.classIs

import scalatags.Text.TypedTag

object UserGuide {

  val firstModel = DocumentationPages.model
  val firstMethod = DocumentationPages.method
  val firstEnvironment = DocumentationPages.environment

  val line = hr(classIs("line"), width := "90%", marginTop := 10)

  def header(sp: TypedTag[_ <: String]) =
    div(minHeight := 300, paddingTop := 120)(
      div(stepHeader)(sp),
      line
    )
  def headerModel(model: String) = header(span(
    tools.to(DocumentationPages.model)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
    span(s"Run your own $model model", h1Like)
  ))

  def headerMethod(method: String) = header(span(
    tools.to(DocumentationPages.method)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
    span(s"Explore with $method", h1Like)
  ))

  def headerEnvironment(env: String) = header(span(
    tools.to(DocumentationPages.environment)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
    span(s"Scale on $env "), h1Like
  ))

  lazy val imgStyle = Seq(
    width := 100,
    paddingRight := 15
  )

  DocumentationPages.dataProcessing
  def currentStep(current: Page): SitePage = {

    val currentStep = {
      if (DocumentationPages.topPages.contains(current)) {
        if ((DocumentationPages.modelPages :+ DocumentationPages.model).contains(current)) {
          val name = if (current == firstModel) "" else current.name
          Step(
            headerModel(name),
            div(current.content),
            SideMenu.model.insert(current.extraMenu).left(350),
            SideMenu.more.insert(current.details).right(350),
            firstModel, firstEnvironment, firstMethod
          )
        }
        else if ((DocumentationPages.methodPages :+ DocumentationPages.method).contains(current))
          Step(
            headerMethod(current.name),
            div(current.content),
            SideMenu.method.insert(current.extraMenu).left(350),
            SideMenu.more.insert(current.details).right(350),
            firstMethod, firstModel, firstEnvironment
          )
        else Step(
          headerEnvironment(current.name),
          div(current.content),
          SideMenu.environment.insert(current.extraMenu).left(350),
          SideMenu.more.insert(current.details).right(350),
          firstEnvironment, firstMethod, firstModel
        )
      }
      else ContentPage(div(paddingTop := 100), div(current.content))
    }

    //    new StepCarousel(
    //      currentStep
    //    ).render

    currentStep
  }

}
