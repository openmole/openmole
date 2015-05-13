package org.openmole.gui.client.core.files

import org.openmole.gui.misc.utils.Utils._
import org.scalajs.dom.raw.HTMLDivElement
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

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

object TreeNodeTabs {

  case class TreeNodeTab(tabName: Var[String], serverFilePath: Var[String], divElement: TypedTag[HTMLDivElement]) {
    val id: String = getUUID
    val active = Var(false)

    def setNameAndPath(name: String, path: String) = {
      tabName() = name
      serverFilePath() = path
    }
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(tabs.toSeq)
}

import org.openmole.gui.client.core.files.TreeNodeTabs._

class TreeNodeTabs(val tabs: Var[Seq[TreeNodeTab]]) {

  def setActive(tab: TreeNodeTab) = {
    unActiveAll
    tab.active() = true
  }

  def unActiveAll = tabs().map {
    _.active() = false
  }

  def ++(tab: TreeNodeTab) = {
    tabs() = tabs() :+ tab
    setActive(tab)
  }

  def --(tab: TreeNodeTab) = {
    tabs() = tabs().filterNot { _ == tab }
  }

  def rename(tn: TreeNode, newName: String) = {
    tabs().find { t ⇒
      t.tabName() == tn.name() && t.serverFilePath() == tn.canonicalPath()
    }.map { tab ⇒
      tab.tabName() = newName
      tab.serverFilePath() = (tab.serverFilePath().split('/').dropRight(1) :+ newName).mkString("/")
    }
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
                  tags.button(`class` := "close", `type` := "button", onclick := { () ⇒ --(t) }
                  )("x"),
                  t.tabName()
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