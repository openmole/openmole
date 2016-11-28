package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.OMTags
import org.openmole.gui.shared._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.utils.Utils._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.misc.utils.stylesheet._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement }
import sheet._
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import scala.scalajs.js.timers._
import org.openmole.gui.misc.js.JsRxTags._

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

    val safePathTab: Var[SafePath]

    val tabName = Var(safePathTab.now.name)
    val id: String = getUUID
    val active: Var[Option[SetIntervalHandle]] = Var(None)

    def desactivate = {
      active.map {
        _.foreach {
          clearInterval
        }
      }
      active() = None
    }

    def activate = {
      active() = Some(setInterval(15000) {
        refresh()
      })
    }

    val editorElement: TypedTag[HTMLDivElement]

    def fileContent: FileContent

    def refresh(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

  }

  trait Save <: TreeNodeTab {
    val editor: EditorPanelUI

    def save(afterSave: () ⇒ Unit) = editor.synchronized {
      OMPost[Api].saveFile(safePathTab.now, editor.code).call().foreach(_ ⇒ afterSave())
    }
  }

  trait Update <: TreeNodeTab {
    val editor: EditorPanelUI

    def update(afterUpdate: () ⇒ Unit) = editor.synchronized {
      FileManager.download(
        safePathTab.now,
        (p: ProcessState) ⇒ {},
        (content: String) ⇒ {
          editor.setCode(content)
          afterUpdate()
        }
      )
    }
  }

  class LockedEditionNodeTab(
      val safePathTab: Var[SafePath],
      val editor:      EditorPanelUI,
      _editable:       Boolean       = false
  ) extends TreeNodeTab with Save with Update {
    val editorElement = editor.view
    val editable = Var(_editable)

    editable.trigger {
      editor.setReadOnly(!editable.now)
    }

    val editButton = Rx {
      if (editable()) tags.div()
      else
        OMTags.glyphBorderButton("", btn_primary +++ editingElement, glyph_edit, () ⇒ {
          editable() = !editable.now
        })
    }

    def controlElement = tags.div(
      if (editable.now) tags.div else editButton
    )

    lazy val overlayElement = tags.div

    def block = div(
      editorElement,
      controlElement,
      overlayElement
    )

    def fileContent = AlterableOnDemandFileContent(safePathTab.now, editor.code, () ⇒ editable.now)

    def refresh(afterRefresh: () ⇒ Unit) = {
      if (editable.now) save(afterRefresh)
      else {
        val scrollPosition = editor.getScrollPostion
        update(() ⇒ {
          afterRefresh()
          editor.setScrollPosition(scrollPosition)
        })
      }
    }
  }

  class HTMLTab(val safePathTab: Var[SafePath], htmlContent: String) extends TreeNodeTab {
    val editorElement = tags.div(
      `class` := "mdRendering",
      RawFrag(htmlContent)
    )

    def fileContent = ReadOnlyFileContent()

    def refresh(onsaved: () ⇒ Unit) = onsaved()
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(Var(tabs.toSeq))

  trait TabControl {
    def controlElement: TypedTag[HTMLElement]
  }

  abstract class OMSTabControl(val safePathTab: Var[SafePath], val editor: EditorPanelUI) extends TabControl with TreeNodeTab with Save {

    val editorElement = editor.view

    def fileContent = AlterableFileContent(safePathTab.now, editor.code)

    def refresh(onsaved: () ⇒ Unit) = save(onsaved)

    val runButton = tags.button("Play", btn_primary)(onclick := { () ⇒ onrun })

    val controlElement = div(executionElement)(runButton)

    val overlaying: Var[Boolean] = Var(false)

    def onrun: Unit

    val block = tabName.flatMap { n ⇒
      overlaying.map { o ⇒
        div(
          div(if (o) playTabOverlay else emptyMod),
          if (o) div(overlayElement)(s"Starting ${n}, please wait ...")
          else div,
          editorElement,
          controlElement
        )
      }
    }

  }

}

import org.openmole.gui.client.core.files.TreeNodeTabs._

class TreeNodeTabs(val tabs: Var[Seq[TreeNodeTab]]) {

  def setActive(tab: TreeNodeTab) = {
    if (tabs.now.contains(tab)) {
      unActiveAll
      tab.activate
    }
  }

  def unActiveAll = tabs.map {
    _.foreach { t ⇒
      t.refresh()
      t.desactivate
    }
  }

  def isActive(tab: TreeNodeTab) = tab.active.map { t ⇒
    t.map {
      _ match {
        case handle: SetIntervalHandle ⇒ true
        case _                         ⇒ false
      }
    }.getOrElse(false)
  }

  def ++(tab: TreeNodeTab) = {
    tabs() = tabs.now :+ tab
    setActive(tab)
  }

  def removeTab(tab: TreeNodeTab) = {
    tab.desactivate
    val newTabs = tabs.now.filterNot {
      _ == tab
    }
    tabs() = newTabs
    newTabs.lastOption.map { t ⇒
      setActive(t)
    }
  }

  def --(tab: TreeNodeTab): Unit = {
    tab.refresh(() ⇒ removeTab(tab))
  }

  def --(safePath: SafePath): Unit = {
    find(safePath).map {
      removeTab
    }
  }

  def alterables: Seq[AlterableFileContent] = tabs.now.map {
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

  def checkTabs = tabs.now.foreach { t: TreeNodeTab ⇒
    OMPost[Api].exists(t.safePathTab.now).call().foreach { e ⇒
      if (!e) removeTab(t)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath) = {
    find(sp).map { tab ⇒
      tab.tabName() = newSafePath.name
      tab.safePathTab() = newSafePath
    }
  }

  def find(safePath: SafePath) = tabs.now.find { t ⇒
    t.safePathTab.now == safePath
  }

  val active = tabs.map {
    _.find { t ⇒
      isActive(t).now
    }
  }

  val render = div({
    div(role := "tabpanel")(
      //Headers
      Rx {
        ul(sheet.nav +++ sheet.navTabs, role := "tablist")(
          for (t ← tabs()) yield {
            li(
              sheet.paddingTop(20),
              role := "presentation",
              `class` := {
                if (isActive(t)()) "active" else ""
              }
            )(
                a(
                  href := "#" + t.id,
                  aria.controls := t.id,
                  role := "tab",
                  data("toggle") := "tab", onclick := { () ⇒
                    setActive(t)
                  }
                )(
                    tags.button(`class` := "close", `type` := "button", onclick := { () ⇒ --(t) })("x"),
                    t.tabName()
                  )
              )
          }
        )
      },
      //Panes
      div(tabContent)(
        Rx {
          for (t ← tabs()) yield {
            val isTabActive = isActive(t)
            div(
              role := "tabpanel",
              ms("tab-pane " + isTabActive.map { a ⇒
                if (a) "active" else ""
              }), id := t.id
            )(if (isTabActive()) {
                t match {
                  case oms: OMSTabControl        ⇒ oms.block
                  case etc: LockedEditionNodeTab ⇒ etc.block
                  case _                         ⇒ Var(div(t.editorElement))
                }
              }
              else div())
          }
        }
      )
    )
  })

}