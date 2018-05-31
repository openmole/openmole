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

  val firstModel = DocumentationPages.run
  val firstMethod = DocumentationPages.explore
  val firstEnvironment = DocumentationPages.scale

  val line = hr(classIs("line"), width := "90%", marginTop := 10)

  def header(sp: TypedTag[_ <: String]) =
    div(minHeight := 250, paddingTop := 100)(
      div(stepHeader)(sp),
      line
    )
  def headerModel(model: String) = header(span(
    tools.to(DocumentationPages.run)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
    span(s"Run your own $model model", h1Like)
  ))

  def headerMethod(method: String) = header(span(
    tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
    span(s"Explore with $method", h1Like)
  ))

  def headerEnvironment(env: String) = header(span(
    tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
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
        if ((DocumentationPages.runPages :+ DocumentationPages.run).contains(current)) {
          val name = if (current == firstModel) "" else current.name
          StepPage(
            headerModel(name),
            div(current.content),
            SideMenu.left(SideMenu.run),
            SideMenu.right(SideMenu.more.insert(current.details)),
            firstModel, firstEnvironment, firstMethod
          )
        }
        else if ((DocumentationPages.explorePages :+ DocumentationPages.explore).contains(current))
          StepPage(
            headerMethod(current.name),
            div(current.content),
            SideMenu.left(SideMenu.explore),
            SideMenu.right(SideMenu.more.insert(current.details)),
            firstMethod, firstModel, firstEnvironment
          )
        else StepPage(
          headerEnvironment(current.name),
          div(current.content),
          SideMenu.left(SideMenu.scale),
          SideMenu.right(SideMenu.more.insert(current.details)),
          firstEnvironment, firstMethod, firstModel
        )
      }
      else ContentPage(div(paddingTop := 100), div(current.content))
    }

    currentStep
  }

}
