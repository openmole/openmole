package org.openmole.gui.misc.js

import org.scalajs.dom.raw.HTMLDivElement
import rx._
import scalatags.JsDom.TypedTag
import org.openmole.gui.misc.utils.Utils._
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.all._

/*
 * Copyright (C) 11/05/15 // mathieu.leclaire@openmole.org
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

object Tabs {

  case class Tab(tabName: String, divElement: TypedTag[HTMLDivElement], id: String = getUUID) {
    val active = Var(false)
  }

  def apply(tabs: Tab*) = new Tabs(tabs.toSeq)
}

import Tabs._

class Tabs(val tabs: Var[Seq[Tab]]) {

  def setActive(tab: Tab) = {
    unActiveAll
    tab.active() = true
  }

  def unActiveAll = tabs().map { _.active() = false }

  def addTab(tab: Tab) = {
    tabs() = tabs() :+ tab
    setActive(tab)
  }

  val render = Rx {
    tags.div(role := "tabpanel")(
      //Headers
      tags.ul(`class` := "nav nav-tabs", role := "tablist")(
        for (t ← tabs()) yield {
          tags.li(role := "presentation",
            `class` := {
              if (t.active()) "active" else ""
            })(
              tags.a(href := "#" + t.id,
                aria.controls := t.id,
                role := "tab",
                data("toggle") := "tab")(
                  t.tabName
                )
            )
        }
      ),
      //Panes
      tags.div(`class` := "tab-content")(
        for (t ← tabs()) yield {
          tags.div(
            role := "tabpanel",
            `class` := "tab-pane fade " + {
              if (t.active()) "in active" else ""
            }, id := t.id
          )(t.divElement.render)
        }
      )
    )
  }

}
