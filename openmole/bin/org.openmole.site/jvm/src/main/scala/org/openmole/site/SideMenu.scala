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

import scalatags.Text.TypedTag
import scalatags.Text.all._

case class Link(name: String, link: String)

case class SideMenu(links: Seq[Link], menuStyle: AttrPair = classIs(""), preText: String = "", otherTab: Boolean = false) {
  def insert(ls: Seq[Link]) = copy(ls ++ links)
}

case class SideMenuBlock(menus: Seq[SideMenu]) {

  def insert(sideMenu: SideMenu): SideMenuBlock = copy(sideMenu +: menus)

  def insert(sideMenu: Option[SideMenu]): SideMenuBlock = {
    sideMenu match {
      case Some(sm: SideMenu) ⇒ insert(sm)
      case _                  ⇒ this
    }
  }

  def insert(links: Seq[Link], menuNumber: Int = 0) = {
    if (menus.size > menuNumber) copy(menus.updated(menuNumber, menus(menuNumber).insert(links)))
    else this
  }

  def add(sideMenu: SideMenu) = copy(menus :+ sideMenu)

  def add(sideMenu: Option[SideMenu]): SideMenuBlock = add(sideMenu.getOrElse(SideMenu(Seq())))

  private def build(topDiv: TypedTag[_]) =
    div(
      topDiv(
        for {
          m ← menus
        } yield {
          div(
            if (m.links.isEmpty) div else div(m.preText, fontWeight := "bold", paddingTop := 20),
            for {
              p ← m.links
            } yield {
              div(paddingTop := 5)(linkButton(p.name, p.link, m.menuStyle, m.otherTab))
            }
          )
        }
      )
    )

  def right(top: Int) = build(div(rightDetailButtons(top)))

  def left(top: Int) = build(div(leftDetailButtons(top)))

}

object SideMenu {

  implicit def pageToLink(p: Page): Link = Link(p.name, p.file)

  implicit def seqPagToSeqLink(ps: Seq[Page]): Seq[Link] = ps.map {
    pageToLink
  }

  def block(sideMenu: SideMenu) = SideMenuBlock(Seq(sideMenu))

  def details(pages: Seq[Page]) = SideMenu(pages, classIs(btn ++ btn_default), otherTab = true)

  def fromStrings(title: String, stringMenus: String*) =
    SideMenu(preText = title, links = stringMenus.map { a ⇒ Link(a, s"#${a.replaceAll(" ", "")}") })

  val model = SideMenu.block(SideMenu(DocumentationPages.modelPages, classIs(btn ++ btn_primary), "Available tasks"))

  val method = SideMenu.block(SideMenu(DocumentationPages.methodPages, classIs(btn ++ btn_primary), "Available methods"))

  val environment = SideMenu.block(SideMenu(DocumentationPages.environmentPages, classIs(btn ++ btn_primary), "Available environments"))

  val more = SideMenu.block(SideMenu(Seq(DocumentationPages.advancedConcepts, DocumentationPages.gui), classIs(btn ++ btn_default), "See also", true))

  lazy val guiGuide = fromStrings(
    "Contents",
    shared.guiGuide.overview,
    shared.guiGuide.startProject,
    shared.guiGuide.fileManagment,
    shared.guiGuide.playAndMonitor,
    shared.guiGuide.authentication,
    shared.guiGuide.plugin
  )

  lazy val clusterMenu = fromStrings(
    "Contents",
    shared.clusterMenu.pbsTorque,
    shared.clusterMenu.sge,
    shared.clusterMenu.slurm,
    shared.clusterMenu.condor,
    shared.clusterMenu.oar
  )

  lazy val nativeMenu = fromStrings(
    "Contents",
    shared.nativeModel.rExample,
    shared.nativeModel.pythonExample,
    shared.nativeModel.advancedOptions
  )

  lazy val otherDoEMenu = fromStrings(
    "Contents",
    shared.otherDoEMenu.basicSampling,
    shared.otherDoEMenu.LHSSobol,
    shared.otherDoEMenu.severalInputs,
    shared.otherDoEMenu.sensitivityAnalysis,
    shared.otherDoEMenu.sensitivityFireModel
  )

  lazy val dataProcessingMenu = fromStrings(
    "Contents",
    shared.dataProcessingMenu.setOfFiles,
    shared.dataProcessingMenu.csvFileSampling
  )

  lazy val advancedSamplingMenu = fromStrings(
    "Contents",
    shared.advancedSamplingMenu.sampling,
    shared.advancedSamplingMenu.combineSampling,
    shared.advancedSamplingMenu.zipSampling,
    shared.advancedSamplingMenu.filterSampling,
    shared.advancedSamplingMenu.randomSampling,
    shared.advancedSamplingMenu.higherLevelSampling,
    shared.advancedSamplingMenu.isKeyword
  )

  lazy val consoleMenu = fromStrings(
    "Contents",
    shared.consoleMenu.run,
    shared.consoleMenu.authentication
  )

}