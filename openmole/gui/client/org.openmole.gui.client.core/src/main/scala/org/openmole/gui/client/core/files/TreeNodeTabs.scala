package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.Post
import org.openmole.gui.shared._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.utils.Utils._
import org.scalajs.dom.raw.HTMLDivElement
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import scala.scalajs.js.timers._

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

  sealed trait TreeNodeTab {
    val tabName: Var[String]

    val serverFilePath: Var[String]

    val id: String = getUUID

    val active: Var[Option[SetIntervalHandle]] = Var(None)

    def desactivate = {
      active().map {
        clearInterval
      }
      active() = None
    }

    val divElement: TypedTag[HTMLDivElement]

    def save(onsaved: () ⇒ Unit = () ⇒ {}): Unit

    def setNameAndPath(name: String, path: String) = {
      tabName() = name
      serverFilePath() = path
    }
  }

  case class EditableNodeTab(tabName: Var[String], serverFilePath: Var[String], editor: EditorPanelUI) extends TreeNodeTab {
    val divElement = editor.view

    def save(onsaved: () ⇒ Unit) = Post[Api].saveFile(serverFilePath(), editor.code).call().foreach { d ⇒
      onsaved()
    }
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(tabs.toSeq)
}

import org.openmole.gui.client.core.files.TreeNodeTabs._

class TreeNodeTabs(val tabs: Var[Seq[TreeNodeTab]]) {

  def setActive(tab: TreeNodeTab) = {
    unActiveAll
    tab.active() = Some(autosaveActive(tab))
  }

  def unActiveAll = tabs().map { t ⇒
    t.save()
    t.desactivate
  }

  def isActive(tab: TreeNodeTab) = tab.active() match {
    case Some(handle: SetIntervalHandle) ⇒ true
    case _                               ⇒ false
  }

  def ++(tab: TreeNodeTab) = {
    tabs() = tabs() :+ tab
    setActive(tab)
  }

  def removeTab(tab: TreeNodeTab) = {
    val isactive = isActive(tab)
    tab.desactivate
    tabs() = tabs().filterNot {
      _ == tab
    }
    if (isactive) tabs().lastOption.map { setActive }
  }

  def --(tab: TreeNodeTab): Unit = tab.save(() ⇒ removeTab(tab))

  def --(treeNode: TreeNode): Unit = find(treeNode).map { removeTab }

  //Autosave the active tab every 15 seconds
  def autosaveActive(tab: TreeNodeTab) = setInterval(15000) {
    tab.save()
  }

  def rename(tn: TreeNode, newName: String) = {
    find(tn).map { tab ⇒
      tab.tabName() = newName
      tab.serverFilePath() = (tab.serverFilePath().split('/').dropRight(1) :+ newName).mkString("/")
    }
  }

  def find(treeNode: TreeNode) = tabs().find { t ⇒
    t.serverFilePath() == treeNode.canonicalPath()
  }

  val render = Rx {
    tags.div(role := "tabpanel")(
      //Headers
      tags.ul(`class` := "nav nav-tabs", role := "tablist")(
        for (t ← tabs()) yield {
          tags.li(role := "presentation",
            `class` := {
              if (isActive(t)) "active" else ""
            })(
              tags.a(href := "#" + t.id,
                aria.controls := t.id,
                role := "tab",
                data("toggle") := "tab", onclick := { () ⇒ setActive(t) })(
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
            `class` := "tab-pane " + {
              if (isActive(t)) "active" else ""
            }, id := t.id
          )(t.divElement.render)
        }
      )
    )
  }

}