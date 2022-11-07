package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.MouseEvent
import scaladget.bootstrapnative.bsn._

import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn

//
///*
// * Copyright (C) 10/08/15 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
class PluginPanel {

  private lazy val plugins: Var[Seq[Plugin]] = Var(Seq())
  getPlugins

  def getPlugins = {
    Fetch(_.listPlugins(()).future) { p ⇒
      plugins.set(p.toSeq)
    }
  }

  val pluginTable =
    div(
      children <-- plugins.signal.combineWith(panels.expandablePanel.signal).map {
        case (ps, _) ⇒
          ps.zipWithIndex.map { case (p, i) ⇒
            div(
              cls := "docEntry",
              backgroundColor := {
                if (i % 2 == 0) "white" else "#ececec"
              },
              div(p.projectSafePath.name, width := "350px"),
              div(
                cls := "badgeOM",
                bsn.badge_dark, p.time
              ), onClick --> { _ ⇒
                panels.treeNodeManager.switch(p.projectSafePath.parent)
                panels.treeNodeManager.computeCurrentSons()
              }
            )
          }
      }
    )

  def render: HtmlElement =
    div(
      div(
        cls := "expandable-title",
        div("Plugins", padding := "10px"),
        div(cls := "close-button bi-chevron-down", onClick --> { _ ⇒ panels.closeExpandable })
      ),
      pluginTable
    )
}