package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.client.core.files.TreeNodeTabs.EditableNodeTab
import org.openmole.gui.ext.data._
import org.openmole.gui.shared._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.utils.Utils._
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import bs._
import org.scalajs.dom.raw.{ HTMLElement, HTMLDivElement }
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

    val serverFilePath: Var[SafePath]

    val id: String = getUUID

    val active: Var[Option[SetIntervalHandle]] = Var(None)

    val overlaying: Var[Boolean] = Var(false)

    def desactivate = {
      active().map {
        clearInterval
      }
      active() = None
    }

    val editorElement: TypedTag[HTMLDivElement]

    val overlayElement: TypedTag[HTMLDivElement] = tags.div(`class` := "overlayElement")(
      tags.div(`class` := "spinner"),
      "Starting " + tabName()
    )

    val tabElement = tags.div()

    val overlayTabElement = tags.div(`class` := "tabOverlay")

    def fileContent: FileContent

    def save(onsaved: () ⇒ Unit = () ⇒ {}): Unit

  }

  class EditableNodeTab(val tabName: Var[String], val serverFilePath: Var[SafePath], editor: EditorPanelUI) extends TreeNodeTab {

    val editorElement = editor.view

    def fileContent = AlterableFileContent(serverFilePath(), editor.code)

    def save(onsaved: () ⇒ Unit) = OMPost[Api].saveFile(serverFilePath(), editor.code).call().foreach { d ⇒
      onsaved()
    }
  }

  class HTMLTab(val tabName: Var[String], val serverFilePath: Var[SafePath], htmlContent: String) extends TreeNodeTab {
    val editorElement = tags.div(`class` := "mdRendering",
      RawFrag(htmlContent)
    )

    def fileContent = ReadOnlyFileContent()

    def save(onsaved: () ⇒ Unit) = onsaved()
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(tabs.toSeq)
}

trait TabControl {
  def controlElement: TypedTag[HTMLElement]
}

trait OMSTabControl <: TabControl {

  val runButton = bs.button("Play", btn_primary)(onclick := { () ⇒
    onrun()
  })

  lazy val controlElement = tags.div(`class` := "executionElement")(
    runButton
  )

  def onrun: () ⇒ Unit

  def relativePath: SafePath
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
    if (isactive) tabs().lastOption.map {
      setActive
    }
  }

  def --(tab: TreeNodeTab): Unit = tab.save(() ⇒ removeTab(tab))

  def --(treeNode: TreeNode): Unit = find(treeNode).map {
    removeTab
  }

  //Autosave the active tab every 15 seconds
  def autosaveActive(tab: TreeNodeTab) = setInterval(15000) {
    tab.save()
  }

  def alterables: Seq[AlterableFileContent] = tabs().map { _.fileContent }.collect {
    case a: AlterableFileContent ⇒ a
  }

  def saveAllTabs(onsave: () ⇒ Unit) = {
    OMPost[Api].saveFiles(alterables).call().foreach { s ⇒
      onsave()
    }
  }

  def checkTabs = tabs().foreach { t: TreeNodeTab ⇒
    OMPost[Api].exists(t.serverFilePath()).call().foreach { e ⇒
      if (!e) removeTab(t)
    }
  }

  def rename(tn: TreeNode, newNode: TreeNode) = {
    find(tn).map { tab ⇒
      tab.tabName() = newNode.name()
      tab.serverFilePath() = newNode.safePath()
    }
  }

  def find(treeNode: TreeNode) = tabs().find { t ⇒
    t.serverFilePath() == treeNode.safePath()
  }

  def active = tabs().find { t ⇒ isActive(t) }

  val render = Rx {
    tags.div(
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
                    tags.button(`class` := "close", `type` := "button", onclick := { () ⇒ println("clicked close"); --(t) }
                    )("x"),
                    t.tabName()
                  )
              )
          }
        ),
        //Panes
        tags.div(`class` := "tab-content")(
          for (t ← tabs()) yield {
            val isTabActive = isActive(t)
            tags.div(
              role := "tabpanel",
              `class` := "tab-pane " + {
                if (isTabActive) "active" else ""
              }, id := t.id
            )(if (isTabActive) {
                active.map { tab ⇒
                  tab match {
                    case oms: TabControl ⇒
                      tags.div(
                        if (t.overlaying()) t.overlayTabElement else t.tabElement,
                        tags.div(
                          t.editorElement,
                          oms.controlElement,
                          if (tab.overlaying()) tab.overlayElement else tags.div
                        )
                      )
                    case _ ⇒ tags.div(t.editorElement)
                  }
                }
              }
              else tags.div()
              )
          }
        )
      )
    )
  }

}