package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.tool.client.Utils._

import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLElement }
import rx._

import scalatags.JsDom.all.{ raw, _ }
import scalatags.JsDom.TypedTag
import scala.scalajs.js.timers._
import org.openmole.gui.ext.tool.client._
import org.openmole.gui.client.core._
import org.openmole.gui.ext.tool.client.FileManager

import net.scalapro.sortable._

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

  sealed trait Activity

  object Active extends Activity

  object UnActive extends Activity

  sealed trait Computation

  object Pending extends Computation

  object StandBy extends Computation

  sealed trait TreeNodeTab {

    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
    val safePathTab: Var[SafePath]
    val activity: Var[Activity] = Var(UnActive)
    val computation: Var[Computation] = Var(StandBy)

    val tabName = Var(safePathTab.now.name)
    val id: String = getUUID

    def activate = activity() = Active

    def desactivate = activity() = UnActive

    def suspend = computation() = Pending

    def standby = computation() = StandBy

    val editorElement: TypedTag[HTMLDivElement]

    def fileContent: FileContent

    def refresh(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

    def block: Rx.Dynamic[TypedTag[_ <: HTMLElement]]

  }

  trait Save <: TreeNodeTab {
    val editor: EditorPanelUI

    def save(afterSave: () ⇒ Unit) = editor.synchronized {
      post()[Api].saveFile(safePathTab.now, editor.code).call().foreach(_ ⇒ afterSave())
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
      if (editable()) div()
      else
        button("Edit", btn_primary +++ executionElement, onclick := { () ⇒
          editable() = !editable.now
        })
    }

    def controlElement = div(
      if (editable.now) div else editButton
    )

    lazy val overlayElement = div

    lazy val block = Rx {
      div(
        editorElement,
        overlayElement
      )
    }

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
    val editorElement =
      div(editorContainer +++ container)(
        overflowY := "scroll",
        div(panelClass +++ panelDefault)(
          div(panelBody)(
            ms("mdRendering") +++ (padding := 10),
            RawFrag(htmlContent)
          )
        )
      )

    def fileContent = ReadOnlyFileContent()

    def refresh(onsaved: () ⇒ Unit) = onsaved()

    lazy val block = Rx {
      editorElement
    }
  }

  def apply(tabs: TreeNodeTab*) = new TreeNodeTabs(Var(tabs.toSeq))

  trait TabControl {
    def controlElement: TypedTag[HTMLElement]
  }

  abstract class OMSTabControl(val safePathTab: Var[SafePath], val editor: EditorPanelUI) extends TabControl with TreeNodeTab with Save {

    val editorElement = editor.view

    def fileContent = AlterableFileContent(safePathTab.now, editor.code)

    def refresh(onsaved: () ⇒ Unit) = save(onsaved)

    val runButton = button("Play", btn_primary, onclick := { () ⇒
      computation() = Pending
      onrun
    })

    val controlElement = div(executionElement)(runButton)

    def onrun: Unit

    val block = {
      computation.flatMap { c ⇒
        tabName.map { n ⇒
          div(
            div(if (c == Pending) playTabOverlay else emptyMod),
            if (c == Pending) div(overlayElement)(s"Starting ${n}, please wait ...")
            else div,
            editorElement
          )
        }
      }
    }
  }

}

import org.openmole.gui.client.core.files.TreeNodeTabs._

class TreeNodeTabs(val tabs: Var[Seq[TreeNodeTab]]) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val timer: Var[Option[SetIntervalHandle]] = Var(None)
  val temporaryControl: Var[Option[TypedTag[HTMLElement]]] = Var(None)

  def stopTimerIfNoTabs = {
    if (tabs.now.isEmpty) {
      timer.map {
        _.foreach {
          clearInterval
        }
      }
      timer() = None
    }
  }

  def startTimerIfStopped =
    timer.now match {
      case None ⇒
        timer() = Some(setInterval(15000) {
          tabs.now.foreach {
            _.refresh()
          }
        })
      case _ ⇒
    }

  def setActive(tab: TreeNodeTab) = {
    if (tabs.now.contains(tab)) {
      unActiveAll
    }
    tab.activate
  }

  def unActiveAll = tabs.map {
    _.foreach { t ⇒
      t.desactivate
    }
  }

  def ++(tab: TreeNodeTab) = {
    tabs() = tabs.now :+ tab
    startTimerIfStopped
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

  def --(tab: TreeNodeTab): Unit = tab.refresh(() ⇒ removeTab(tab))

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
    org.openmole.gui.client.core.post()[Api].saveFiles(alterables).call().foreach { s ⇒
      onsave()
    }
  }

  def checkTabs = tabs.now.foreach { t: TreeNodeTab ⇒
    org.openmole.gui.client.core.post()[Api].exists(t.safePathTab.now).call().foreach { e ⇒
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

  def set(safePath: SafePath, c: Computation) = {
    find(safePath).foreach {
      _.computation() = c
    }
  }

  implicit def modToModSeq(m: Modifier): ModifierSeq = Seq(m)

  val render = div({
    div(role := "tabpanel")(
      //Headers
      Rx {
        val tabList = ul(nav +++ navTabs, tab_list_role)(
          for (t ← tabs()) yield {
            li(
              paddingTop := 35,
              presentation_role,
              `class` := {
                t.activity() match {
                  case Active ⇒ "active"
                  case _      ⇒ ""
                }
              }
            )(
                a(
                  href := "#" + t.id,
                  aria.controls := t.id,
                  tab_role,
                  t.activity() match {
                    case Active ⇒ activeTab
                    case _      ⇒ unActiveTab
                  },
                  data("toggle") := "tab", onclick := { () ⇒
                    setActive(t)
                  }
                )(
                    button(ms("close") +++ tabClose, `type` := "button", onclick := { () ⇒ --(t) })(raw("&#215")),
                    t.tabName()
                  )
              )
          }
        ).render

        new Sortable(tabList, new SortableProps {
          override val onEnd = scala.scalajs.js.defined {
            (event: EventS) ⇒
              val oldI = event.oldIndex.asInstanceOf[Int]
              val newI = event.newIndex.asInstanceOf[Int]
              tabs() = tabs.now.updated(oldI, tabs.now(newI)).updated(newI, tabs.now(oldI))
              setActive(tabs.now(newI))
          }
        })
        tabList
      },
      //Panes
      div(tabContent)(
        Rx {
          for (t ← tabs()) yield {
            div(
              role := "tabpanel",
              ms("tab-pane " + {
                t.activity() match {
                  case Active ⇒ "active"
                  case _      ⇒ ""
                }
              }), id := t.id
            )({
                t.activity() match {
                  case Active ⇒
                    t match {
                      case oms: OMSTabControl ⇒
                        temporaryControl() = Some(oms.computation() match {
                          case Pending ⇒ div()
                          case _       ⇒ oms.controlElement
                        })
                        oms.block()
                      case etc: LockedEditionNodeTab ⇒
                        temporaryControl() = Some(etc.controlElement)
                        etc.block()
                      case _ ⇒
                        temporaryControl() = None
                        Var(div(t.editorElement))
                    }
                  case UnActive ⇒
                    t.computation() match {
                      case Pending ⇒ Waiter.waiter
                      case _       ⇒ div()
                    }

                }
              })
          }
        }
      )
    )
  })

}
