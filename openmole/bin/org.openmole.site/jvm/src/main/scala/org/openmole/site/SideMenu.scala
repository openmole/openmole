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

case class SideMenu(pages: Seq[Page], menuStyle: AttrPair = classIs(btn ++ btn_default), preText: String = "", otherTab: Boolean = false)

case class SideMenuBlock(menus: Seq[SideMenu]) {

  def add(sideMenu: SideMenu) = copy(menus :+ sideMenu)

  private def build(topDiv: TypedTag[_]) =
    div(
      topDiv(
        for {
          m ← menus
        } yield {
          div(
            if (m.pages.isEmpty) div else div(m.preText, fontWeight := "bold", paddingTop := 20),
            for {
              p ← m.pages
            } yield {
              div(paddingTop := 10)(linkButton(p.name, p.file, m.menuStyle, m.otherTab))
            }
          )
        }
      )
    )

  def right(top: Int) = build(div(rightDetailButtons(top)))

  def left(top: Int) = build(div(leftDetailButtons(top)))

}

object SideMenu {

  def block(sideMenu: SideMenu) = SideMenuBlock(Seq(sideMenu))

  def details(pages: Seq[Page]) = SideMenu(pages, preText = "See also", otherTab = true)

  val model = SideMenu.block(SideMenu(DocumentationPages.modelPages, classIs(btn ++ btn_primary), "Available tasks"))

  val method = SideMenu.block(SideMenu(DocumentationPages.methodPages, classIs(btn ++ btn_primary), "Available methods"))

  val environment = SideMenu.block(SideMenu(DocumentationPages.environmentPages, classIs(btn ++ btn_primary), "Available environments"))
}