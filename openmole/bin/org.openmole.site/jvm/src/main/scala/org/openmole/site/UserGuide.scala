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
      span(s"Run your own $model model", h1Like)
    ))
  }

  def headerMethod(method: String) = method match {
    case "Explore" ⇒ header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore your model", h1Like)
    ))
    case _ ⇒ header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore with the $method method", h1Like)
    ))
  }

  def headerEnvironment(env: String) = env match {
    case "Scale" ⇒ header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale on different environments"), h1Like))
    case _ ⇒ header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale on $env"), h1Like
    ))
  }

  lazy val imgStyle = Seq(
    width := 100,
    paddingRight := 15
  )

  def integrate(current: Page): SitePage =
    current match {
      case p if (DocumentationPages.runPages :+ DocumentationPages.run).contains(p) ⇒
        IntegratedPage(
          headerModel(current.name),
          div(current.content),
          SideMenu.left(SideMenu.run),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.explorePages :+ DocumentationPages.explore).contains(p)) ⇒
        IntegratedPage(
          headerMethod(current.name),
          div(current.content),
          SideMenu.left(SideMenu.explore),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.scalePages :+ DocumentationPages.scale).contains(p)) ⇒
        IntegratedPage(
          headerEnvironment(current.name),
          div(current.content),
          SideMenu.left(SideMenu.scale),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.languagePages :+ DocumentationPages.language).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.language),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.advancedConceptsPages :+ DocumentationPages.advancedConcepts).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.advancedConcepts),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.developersPages :+ DocumentationPages.developers).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.developers),
          Some(SideMenu.right(SideMenu.more.insert(current.details)))
        )
      case p if ((DocumentationPages.gettingStartedPages).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.gettingStarted),
          None
        )
      case p if ((DocumentationPages.netLogoPages).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.netLogoGA),
          None
        )
      case p if ((DocumentationPages.communityPages :+ DocumentationPages.OMcommunity).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.community),
          None
        )
      case p if ((DocumentationPages.downloadPages :+ DocumentationPages.download).contains(p)) ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu.download),
          None
        )
      case _ ⇒
        IntegratedPage(
          div(paddingTop := 100),
          div(current.content),
          SideMenu.left(SideMenu(Seq.empty, classIs(btn ++ btn_primary))),
          None
        )
    }

}
