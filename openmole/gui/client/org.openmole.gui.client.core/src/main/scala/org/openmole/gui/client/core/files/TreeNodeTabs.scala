package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.OMTags
import org.openmole.gui.shared._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.utils.Utils._
import org.openmole.gui.misc.js.JsRxTags._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.scalajs.dom.raw.{ HTMLElement, HTMLDivElement }
import rx._
import bs._

import scala.util.{ Failure, Success }
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

    val treeNode: TreeNode

    val tabName: Var[String] = treeNode.name()
    val id: String = getUUID
    val active: Var[Option[SetIntervalHandle]] = Var(None)

    def desactivate = {
      active().foreach { clearInterval }
      active() = None
    }

    def activate = {
      active() = Some(setInterval(15000) { refresh() })
    }

    val editorElement: TypedTag[HTMLDivElement]

    val tabElement = tags.div()

    def fileContent: FileContent

    def refresh(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

  }

  trait Save <: TreeNodeTab {
    val editor: EditorPanelUI

    def save(afterSave: () ⇒ Unit) = editor.synchronized {
      OMPost[Api].saveFile(treeNode.safePath(), editor.code).call().foreach(_ ⇒ afterSave())
    }
  }

  trait Update <: TreeNodeTab {
    val editor: EditorPanelUI

    def update(afterUpdate: () ⇒ Unit) = editor.synchronized {
      FileManager.download(
        treeNode,
        (p: ProcessState) ⇒ {},
        (content: String) ⇒ {
          editor.setCode(content)
          afterUpdate()
        }
      )
    }
  }

  class EditableNodeTab(val treeNode: TreeNode, val editor: EditorPanelUI) extends TreeNodeTab with Save {

    val editorElement = editor.view

    def fileContent = AlterableFileContent(treeNode.safePath(), editor.code)

    def refresh(onsaved: () ⇒ Unit) = save(onsaved)
  }

  class LockedEditionNodeTab(
      val treeNode: TreeNode,
      val editor:   EditorPanelUI,
      _editable:    Boolean       = false
  ) extends TreeNodeTab with Save with Update {
    val editorElement = editor.view
    val editable = Var(_editable)

    Obs(editable) {
      editor.setReadOnly(!editable())
    }

    val editButton = OMTags.glyphBorderButton("", btn_primary + "editingElement", glyph_edit, () ⇒ {
      editable() = !editable()
    })

    def controlElement = tags.div(
      if (editable()) tags.div else editButton
    )

    lazy val overlayElement = tags.div

    def fileContent = AlterableOnDemandFileContent(treeNode.safePath(), editor.code, () ⇒ editable())

    def refresh(afterRefresh: () ⇒ Unit) =
      if (editable()) save(afterRefresh)
      else {
        val scrollPosition = editor.getScrollPostion
        update(() ⇒ {
          afterRefresh()
          editor.setScrollPosition(scrollPosition)
        })
      }
  }

  class HTMLTab(val treeNode: TreeNode, htmlContent: String) extends TreeNodeTab {
    val editorElement = tags.div(
      `class` := "mdRendering",
      RawFrag(htmlContent)
    )

    def fileContent = ReadOnlyFileContent()

    def refresh(onsaved: () ⇒ Unit) = onsaved()
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(tabs.toSeq)
}

trait TabControl {
  def controlElement: TypedTag[HTMLElement]
}

trait OMSTabControl <: TabControl {

  def node: TreeNode

  val runButton = bs.button("Play", btn_primary)(onclick := { () ⇒
    onrun()
  })

  lazy val controlElement = tags.div(`class` := "executionElement")(
    runButton
  )

  val overlayTabElement = tags.div(`class` := "playTabOverlay")

  val overlayElement: TypedTag[HTMLDivElement] = tags.div(`class` := "overlayElement")(
    s"Starting ${node.name()}, please wait ..."
  )

  val overlaying: Var[Boolean] = Var(false)

  def onrun: () ⇒ Unit

  def relativePath: SafePath
}

import org.openmole.gui.client.core.files.TreeNodeTabs._

class TreeNodeTabs(val tabs: Var[Seq[TreeNodeTab]]) {

  def setActive(tab: TreeNodeTab) = {
    unActiveAll
    tab.activate
  }

  def unActiveAll = tabs().map { t ⇒
    t.refresh()
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

  def --(tab: TreeNodeTab): Unit = tab.refresh(() ⇒ removeTab(tab))

  def --(treeNode: TreeNode): Unit = find(treeNode).map {
    removeTab
  }

  def alterables: Seq[AlterableFileContent] = tabs().map {
    _.fileContent
  }.collect {
    case a: AlterableFileContent                               ⇒ a
    case aod: AlterableOnDemandFileContent if (aod.editable()) ⇒ AlterableFileContent(aod.path, aod.content)
  }

  def saveAllTabs(onsave: () ⇒ Unit) = {
    OMPost[Api].saveFiles(alterables).call().foreach { s ⇒
      onsave()
    }
  }

  def checkTabs = tabs().foreach { t: TreeNodeTab ⇒
    OMPost[Api].exists(t.treeNode.safePath()).call().foreach { e ⇒
      if (!e) removeTab(t)
    }
  }

  def rename(tn: TreeNode, newNode: TreeNode) = {
    find(tn).map { tab ⇒
      tab.tabName() = newNode.name()
      tab.treeNode.safePath() = newNode.safePath()
    }
  }

  def find(treeNode: TreeNode) = tabs().find { t ⇒
    t.treeNode.safePath() == treeNode.safePath()
  }

  def active = tabs().find { t ⇒ isActive(t) }

  val render = Rx {
    tags.div(
      tags.div(role := "tabpanel")(
        //Headers
        tags.ul(`class` := "nav nav-tabs", role := "tablist")(
          for (t ← tabs()) yield {
            tags.li(
              role := "presentation",
              `class` := {
                if (isActive(t)) "active" else ""
              }
            )(
                tags.a(
                  href := "#" + t.id,
                  aria.controls := t.id,
                  role := "tab",
                  data("toggle") := "tab", onclick := { () ⇒ setActive(t) }
                )(
                    tags.button(`class` := "close", `type` := "button", onclick := { () ⇒ --(t) })("x"),
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
                    case oms: OMSTabControl ⇒
                      tags.div(
                        if (oms.overlaying()) oms.overlayTabElement else oms.tabElement,
                        tags.div(
                          oms.editorElement,
                          oms.controlElement,
                          if (oms.overlaying()) oms.overlayElement else tags.div
                        )
                      )
                    case etc: LockedEditionNodeTab ⇒
                      println("ETC " + etc.editable() + " " + etc.controlElement.toString)
                      tags.div(
                        etc.tabElement,
                        tags.div(
                          etc.overlayElement,
                          etc.editorElement,
                          etc.controlElement
                        )
                      )
                    case _ ⇒ tags.div(tab.editorElement)
                  }
                }
              }
              else tags.div())
          }
        )
      )
    )
  }

}