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

import org.openmole.site
import tools._
import stylesheet._

import scalatags.Text.TypedTag
import scalatags.Text.all._

object Link {
  def intern(name: String) = s"#${name.replaceAll(" ", "")}"
}

case class Link(name: String, link: String)

case class SideMenu(links: Seq[Link], menuStyle: AttrPair = classIs(""), preText: String = "", otherTab: Boolean = false, currentPage: Option[Page] = None, topLink: Option[String] = None) {
  def insert(ls: Seq[Link]) = copy(ls ++ links)
}

object SideMenu {

  private def build(menus: Seq[SideMenu], topDiv: TypedTag[_], extraDiv: Option[Frag] = None, currentPage: Option[Page] = None, selectMode: Boolean = false) =
    div(
      topDiv(
        extraDiv,
        for { m ← menus } yield {
          div(
            if (m.links.isEmpty) div
            else {
              val text: ConcreteHtmlTag[String] =
                m.topLink match {
                  case Some(l) => a(href := l, m.preText)
                  case None => a(m.preText)
                }
              div(text, fontWeight := "bold", paddingBottom := 10)
            },
            for { p ← m.links } yield {
              val basicButton = div(paddingTop := 10)(linkButton(p.name, p.link, m.menuStyle, m.otherTab))
              currentPage match {
                case Some(page) => {
                  if ((page.name == p.name) && selectMode) div(paddingTop := 10)(linkButton(p.name, p.link, openInOtherTab = m.otherTab))
                  else basicButton
                }
                case _ => basicButton
              }
            }
          )
        }
      )
    )

  def right(current: Page, menus: SideMenu*) = build(menus, div(rightDetailButtons(150), id := "sidebar-right"), currentPage = Some(current))

  def left(current: Page, menus: SideMenu*) = build(menus, div(leftDetailButtons(280), `class` := "sidebar-left"), currentPage = Some(current), selectMode = true)

  implicit def pageToLink(p: Page): Link = Link(p.name, p.file)

  implicit def seqPagToSeqLink(ps: Seq[Page]): Seq[Link] = ps.map {
    pageToLink
  }

  implicit def pageNodeToLink(pageTree: PageTree): Seq[Link] = pageTree.sons.map { pageToLink(_) }

  def details(pages: Seq[Page]) = SideMenu(pages, classIs(btn, btn_default), otherTab = true)

//  def fromStrings(title: String, stringMenus: String*) =
//    SideMenu(preText = title, links = stringMenus.map { a => Link(a, Link.intern(a)) })

  val plug = SideMenu(DocumentationPages.plugPages, classIs(btn, btn_default), "Available tasks", topLink = Some(DocumentationPages.plug.link))
  val explore = SideMenu(DocumentationPages.explorePages, classIs(btn, btn_default), "Available methods", topLink = Some(DocumentationPages.explore.link))
  val sampling = SideMenu(DocumentationPages.samplingPages, classIs(btn, btn_default), "Sampling methods", topLink = Some(DocumentationPages.samplings.link))
  val scale = SideMenu(DocumentationPages.scalePages, classIs(btn, btn_default), "Available environments", topLink = Some(DocumentationPages.scale.link))
  val language = SideMenu(DocumentationPages.languagePages, classIs(btn, btn_default), "Language", topLink = Some(DocumentationPages.language.link))
  val advanced = SideMenu(DocumentationPages.advancedConceptsPages, classIs(btn, btn_default), "Advanced concepts", topLink = Some(DocumentationPages.advancedConcepts.link))
  val developers = SideMenu(DocumentationPages.developersPages, classIs(btn, btn_default), "Developer's documentation", topLink = Some(DocumentationPages.developers.link))
  val tutorials = SideMenu(DocumentationPages.tutoPages, classIs(btn, btn_default), "Tutorials", topLink = Some(DocumentationPages.tutorials.link))
  val community = SideMenu(DocumentationPages.communityPages, classIs(btn, btn_default), "Community", topLink = Some(DocumentationPages.OMcommunity.link))
  val download = SideMenu(DocumentationPages.downloadPages, classIs(btn, btn_default), "Download", topLink = Some(DocumentationPages.download.link))

  def more(current: Page) = SideMenu(
    Seq(
      DocumentationPages.plug,
      DocumentationPages.explore,
      DocumentationPages.scale,
      DocumentationPages.language,
      DocumentationPages.geneticAlgorithm,
      DocumentationPages.developers,
      DocumentationPages.gui
    ).filterNot(_ == current),
    classIs(""),
    "See also in the doc"
  )

}
