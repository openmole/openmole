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
import tools._

import scalatags.Text.TypedTag

object UserGuide {
  /*
  val firstModel = DocumentationPages.run
  val firstMethod = DocumentationPages.explore
  val firstEnvironment = DocumentationPages.scale
*/
  val line = hr(classIs("line"), width := "90%", marginTop := 10)

  def header(sp: TypedTag[_ <: String]) =
    div(minHeight := 250, paddingTop := 100)(
      div(stepHeader)(sp),
      line
    )

  def headerModel(model: String) = model match {
    case "Run" ⇒ header(span(
      tools.to(DocumentationPages.run)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
      span(s"Run your model", h1Like)
    ))
    case _ ⇒ header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
      span(s"Run your $model model", h1Like)
    ))
  }

  def headerMethod(method: String) = method match {
    case "Explore" ⇒ header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore your model", h1Like)
    ))
    case _ ⇒ header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore: $method", h1Like)
    ))
  }

  def headerEnvironment(env: String) = env match {
    case "Scale" ⇒ header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale on different environments"), h1Like))
    case _ ⇒ header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale: $env"), h1Like
    ))
  }

  lazy val imgStyle = Seq(
    width := 100,
    paddingRight := 15
  )

  def integrate(current: Page): SitePage = {
    def integratedPage(left: SideMenu, right: SideMenu = SideMenu.more, head: TypedTag[String] = div(paddingTop := 100)) =
      IntegratedPage(
        head,
        div(current.content),
        SideMenu.left(left),
        Some(SideMenu.right(right.insert(current.details)))
      )

    current match {
      case p if (DocumentationPages.runPages :+ DocumentationPages.run).contains(p) ⇒ integratedPage(SideMenu.run, head = headerModel(current.name))
      case p if (DocumentationPages.explorePages :+ DocumentationPages.explore).contains(p) ⇒ integratedPage(SideMenu.explore, head = headerMethod(current.name))
      case p if (DocumentationPages.scalePages :+ DocumentationPages.scale).contains(p) ⇒ integratedPage(SideMenu.scale, head = headerEnvironment(current.name))
      case p if (DocumentationPages.advancedConceptsPages :+ DocumentationPages.advancedConcepts).contains(p) ⇒ integratedPage(SideMenu.advanced)
      case p if (DocumentationPages.developersPages :+ DocumentationPages.developers).contains(p) ⇒ integratedPage(SideMenu.developers)
      case p if (DocumentationPages.languagePages :+ DocumentationPages.language).contains(p) ⇒ integratedPage(SideMenu.language)
      case p if (DocumentationPages.gettingStartedPages).contains(p) ⇒ integratedPage(SideMenu.gettingStarted)
      case p if (DocumentationPages.netLogoPages).contains(p) ⇒ integratedPage(SideMenu.netLogoGA)
      case p if (DocumentationPages.communityPages :+ DocumentationPages.OMcommunity).contains(p) ⇒ integratedPage(SideMenu.community)
      case p if (DocumentationPages.downloadPages :+ DocumentationPages.download).contains(p) ⇒ integratedPage(SideMenu.download)
      case _ ⇒ integratedPage(SideMenu(Seq.empty, classIs(btn ++ btn_primary)))
    }
  }

}
