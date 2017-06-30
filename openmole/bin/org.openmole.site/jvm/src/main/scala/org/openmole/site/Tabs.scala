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
import scalatags.Text.all._

import scalatags.Text.TypedTag

case class Tab(title: String, content: TypedTag[_ <: String], active: Boolean, topage: Page) {
  def activeClass = if (active) (classIs("active"), classIs("active in")) else (classIs(""), classIs(""))
}

case class Tabs(tabs: Seq[Tab] = Seq()) {

  def add(title: String, content: TypedTag[_ <: String], active: Boolean = false, topage: Page): Tabs = add(Tab(title, content, active, topage))

  def add(tab: Tab): Tabs = copy(tabs = tabs :+ tab)

  lazy val render = {

    val existsOneActive = tabs.map {
      _.active
    }.exists(_ == true)
    val theTabs = {
      if (!existsOneActive && !tabs.isEmpty) tabs.head.copy(active = true) +: tabs.tail
      else tabs
    }

    div(
      ul(classIs(nav ++ nav_pills), role_tablist)(
        theTabs.map { t ⇒
          li(role_presentation, t.activeClass._1)(
            tools.to(t.topage)(t.title)
          )
        }
      ),
      div(classIs("tab_content"), paddingTop := 10)(
        theTabs.map { t ⇒
          div(t.content)
        }
      )
    )
  }
}