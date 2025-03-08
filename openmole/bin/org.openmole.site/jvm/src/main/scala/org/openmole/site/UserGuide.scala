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

import scalatags.Text.TypedTag

object UserGuide {

  val line = hr(tools.classIs("line"), width := "90%", marginTop := 10)

  implicit def fromPageTreeToLinks(pageTree: PageTree): Seq[Link] = pageTree.sons.map { s => Link(s.page.name, s.page.file) }

  def header(sp: TypedTag[_ <: String]) =
    div(paddingTop := 40)(
      div(stepHeader)(sp),
      line
    )

  def headerModel(model: String) = model match {
    case "Plug" => header(span(
      tools.to(DocumentationPages.plug)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
      span(s"Plug your model", tools.classIs("h1"))
    ))
    case _ => header(span(
      tools.to(DocumentationPages.plug)(img(src := Resource.img.model.codeAnimated.file, headerImg)),
      span(s"Plug your $model model", tools.classIs("h1"))
    ))
  }

  def headerMethod(method: String) = method match {
    case "Explore" => header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore your model", tools.classIs("h1"))
    ))
    case _ => header(span(
      tools.to(DocumentationPages.explore)(img(src := Resource.img.method.exploreMapAnimated.file, headerImg)),
      span(s"Explore with $method", tools.classIs("h1"))
    ))
  }

  def headerEnvironment(env: String) = env match {
    case "Scale Up" => header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale up your experiments"), tools.classIs("h1")))
    case _ => header(span(
      tools.to(DocumentationPages.scale)(img(src := Resource.img.environment.scaleAnimated.file, headerImg)),
      span(s"Scale up on $env"), tools.classIs("h1")
    ))
  }

  lazy val imgStyle = Seq(
    width := 100,
    paddingRight := 15
  )

  def h2Contents(content: String) = {
    val parsing = scala.xml.XML.loadString(s"<html>$content</html>")
    val h2s = (parsing \\ "h2").zipWithIndex.map {
      case (h, i) =>
        val text = h.text.replaceAll("\uD83D\uDD17", "")
        div(paddingTop := 5, paddingLeft := 10)(a(href := "#" + shared.anchor(text))(s"${i + 1} - $text"))
    }

    if (h2s.size <= 1) span(marginTop := 40) else div(marginBottom := 30, scalatags.Text.all.h2("Content:"), h2s, br, hr)
  }

  def integrate(current: PageTree): SitePage = {
    val parentPageTrees = PageTree.parents(current)
    def integratedPage(left: SideMenu, right: SideMenu = SideMenu.more(current.page), head: TypedTag[String] = header(span(current.title, h1Like))(textAlign := "center", marginBottom := 30)) = {
      IntegratedPage(
        head,
        div(
          h2Contents(current.content.render),
          current.content
        ),
        SideMenu.left(current, left),
        Some(SideMenu.right(current, right /*.insert(current.details)*/ )),
        PageTree.fromPage(DocumentationPages.documentation) +: parentPageTrees.reverse
      )
    }

    val parents = parentPageTrees.map { _.name }

    current match {
      case p if (parents.contains(DocumentationPages.plug.name) || current.name == DocumentationPages.plug.name) => integratedPage(SideMenu.plug, head = headerModel(current.name))
      case p if ((DocumentationPages.samplingPages.sons.map { _.name } :+ DocumentationPages.samplings.name).contains(current.name)) => integratedPage(SideMenu.sampling, head = headerMethod(current.name))
      case p if (parents.contains(DocumentationPages.explore.name) || current.name == DocumentationPages.explore.name) => integratedPage(SideMenu.explore, head = headerMethod(current.name))
      case p if (parents.contains(DocumentationPages.scale.name) || current.name == DocumentationPages.scale.name) => integratedPage(SideMenu.scale, head = headerEnvironment(current.name))
      case p if (parents.contains(DocumentationPages.advancedConcepts.name) || current.name == DocumentationPages.advancedConcepts.name) => integratedPage(SideMenu.advanced)
      case p if (parents.contains(DocumentationPages.developers.name) || current.name == DocumentationPages.developers.name) => integratedPage(SideMenu.developers)
      case p if (parents.contains(DocumentationPages.language.name) || current.name == DocumentationPages.language.name) => integratedPage(SideMenu.language)
      case p if (parents.contains(DocumentationPages.tutorials.name) || current.name == DocumentationPages.tutorials.name) => integratedPage(SideMenu.tutorials)
      case p if (parents.contains(DocumentationPages.OMcommunity.name) || current.name == DocumentationPages.OMcommunity.name) => integratedPage(SideMenu.community)
      case p if (parents.contains(DocumentationPages.download.name) || current.name == DocumentationPages.download.name) => integratedPage(SideMenu.download)
      case _ => integratedPage(SideMenu(Seq.empty, tools.classIs(tools.btn, tools.btn_primary)))
    }
  }

}
