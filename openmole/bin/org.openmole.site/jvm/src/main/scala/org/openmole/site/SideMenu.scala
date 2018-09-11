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

case class SideMenu(links: Seq[Link], menuStyle: AttrPair = classIs(""), preText: String = "", otherTab: Boolean = false, currentPage: Option[Page] = None) {
  def insert(ls: Seq[Link]) = copy(ls ++ links)
}

object SideMenu {

  private def build(menus: Seq[SideMenu], topDiv: TypedTag[_], extraDiv: Option[Frag] = None, currentPage: Option[Page] = None) =
    div(
      topDiv(
        extraDiv,
        for { m ← menus } yield {
          div(
            if (m.links.isEmpty) div else div(m.preText, fontWeight := "bold", paddingBottom := 5),
            for { p ← m.links } yield {
              val basicButton = div(paddingTop := 5)(linkButton(p.name, p.link, m.menuStyle, m.otherTab))
              currentPage match {
                case Some(page) ⇒ {
                  if (page.name == p.name) div(paddingTop := 5)(linkButton(p.name, p.link, openInOtherTab = m.otherTab))
                  else basicButton
                }
                case _ ⇒ basicButton
              }
            }
          )
        }
      )
    )

  def right(menus: SideMenu*) = build(menus, div(rightDetailButtons(180), id := "sidebar-right"))

  def left(current: Page, menus: SideMenu*) = {
    build(menus, div(leftDetailButtons(180), `class` := "sidebar-left"), currentPage = Some(current))
  }

  implicit def pageToLink(p: Page): Link = Link(p.name, p.file)

  implicit def seqPagToSeqLink(ps: Seq[Page]): Seq[Link] = ps.map {
    pageToLink
  }

  def details(pages: Seq[Page]) = SideMenu(pages, classIs(btn ++ btn_default), otherTab = true)

  def fromStrings(title: String, stringMenus: String*) =
    SideMenu(preText = title, links = stringMenus.map { a ⇒ Link(a, Link.intern(a)) })

  val run = SideMenu(DocumentationPages.runPages, classIs(btn ++ btn_primary), "Available tasks")
  val packaged = SideMenu(DocumentationPages.packagedPages, classIs(btn ++ btn_primary), "Package your code")
  val explore = SideMenu(DocumentationPages.explorePages, classIs(btn ++ btn_primary), "Available methods")
  val sensitivity = SideMenu(DocumentationPages.sensitivityPages, classIs(btn ++ btn_primary), "Sensitivity methods")
  val sampling = SideMenu(DocumentationPages.samplingPages, classIs(btn ++ btn_primary), "Sampling methods")
  val scale = SideMenu(DocumentationPages.scalePages, classIs(btn ++ btn_primary), "Available environments")
  val language = SideMenu(DocumentationPages.languagePages, classIs(btn ++ btn_primary), "Language")
  val advanced = SideMenu(DocumentationPages.advancedConceptsPages, classIs(btn ++ btn_primary), "Advanced concepts")
  val developers = SideMenu(DocumentationPages.developersPages, classIs(btn ++ btn_primary), "Developer's documentation")
  val gettingStarted = SideMenu(DocumentationPages.gettingStartedPages, classIs(btn ++ btn_primary), "Getting started tutorials")
  val netLogoGA = SideMenu(DocumentationPages.netLogoPages, classIs(btn ++ btn_primary), "NetLogo tutorials")
  val community = SideMenu(DocumentationPages.communityPages, classIs(btn ++ btn_primary), "Community")
  val download = SideMenu(DocumentationPages.downloadPages, classIs(btn ++ btn_primary), "Download")

  val more = SideMenu(
    Seq(
      DocumentationPages.run,
      DocumentationPages.explore,
      DocumentationPages.scale,
      DocumentationPages.language,
      DocumentationPages.advancedConcepts,
      DocumentationPages.developers,
      DocumentationPages.gui
    ),
    classIs(btn ++ btn_default),
    "See also in the doc"
  )

}
