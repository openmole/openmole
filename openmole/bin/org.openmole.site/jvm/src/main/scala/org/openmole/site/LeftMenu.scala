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

case class LeftMenu(pages: Seq[Page], menuStyle: AttrPair = classIs(btn ++ btn_default), preText: String = "", otherTab: Boolean = false)

case class LeftMenuBlock(menus: Seq[LeftMenu]) {

  def add(leftMenu: LeftMenu) = copy(menus :+ leftMenu)

  def build(top: Int) =
    div(
      div(detailButtons(top))(
        for {
          m ← menus
        } yield {
          div(
            if (m.pages.isEmpty) div else div(m.preText, fontWeight := "bold", paddingTop := 40),
            for {
              p ← m.pages
            } yield {
              div(paddingTop := 10)(linkButton(p.name, p.file, m.menuStyle, m.otherTab))
            }
          )
        }
      )
    )

}

object LeftMenu {

  def block(leftMenu: LeftMenu) = LeftMenuBlock(Seq(leftMenu))

  def details(pages: Seq[Page]) = LeftMenu(pages, preText = "See also", otherTab = true)

  val model = LeftMenu.block(LeftMenu(DocumentationPages.modelPages, classIs(btn ++ btn_primary), "Available tasks"))

  val method = LeftMenu.block(LeftMenu(DocumentationPages.methodPages, classIs(btn ++ btn_primary), "Available methods"))

  val environment = LeftMenu.block(LeftMenu(DocumentationPages.environmentPages, classIs(btn ++ btn_primary), "Available environments"))
}