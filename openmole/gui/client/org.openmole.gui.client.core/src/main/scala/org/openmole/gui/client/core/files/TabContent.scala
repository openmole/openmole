package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Fetch, Panels}
import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TreeNodeTab.{save, serverConflictAlert}
import org.openmole.gui.client.ext.ServerAPI
import scaladget.tools.Utils.uuID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}


object TabContent:
  case class TabData(safePath: SafePath, editorPanelUI: Option[EditorPanelUI])

class TabContent:

  import TabContent.TabData
  val tabsUI = Tabs.tabs[TabData](Seq()).build

  def render(using panels: Panels, api: ServerAPI) =
    val timer: Var[Option[SetIntervalHandle]] = Var(None)

    def tabsObserver =
      Observer[Seq[Tab[TabData]]] { tabs =>
        if (tabs.isEmpty) {
          timer.now().foreach { handle => clearInterval(handle) }
          timer.set(None)
        }
      }

    def timerObserver =
      Observer[Option[SetIntervalHandle]] {
        case None =>
          timer.set(Some(setInterval(15000) {tabsUI.tabs.now().foreach { t => save(t.t) } }))
        case _ =>
      }

    tabsUI.render.amend(
      margin := "10px",
      tabsUI.tabs --> tabsObserver,
      timer --> timerObserver
    )

  private def buildHeader(tabData: TabData, onRemoved: SafePath => Unit, onClicked: SafePath => Unit)(using panels: Panels, api: ServerAPI) = {
    span(display.flex, flexDirection.row, alignItems.center,
      span(tabData.safePath.name),
      span(cls := "close-button close-button-tab bi-x", marginLeft := "5px", onClick --> { e =>
        save(tabData)
        removeTab(tabData.safePath)
        onRemoved(tabData.safePath)
        e.stopPropagation()
      }),
      onClick --> { _ => onClicked(tabData.safePath) }
    )
  }

  def addTab(
    tabData: TabData,
    content: HtmlElement,
    onClicked: SafePath => Unit = _ => {},
    onAdded: SafePath => Unit = _ => {},
    onRemoved: SafePath => Unit = _ => {})(using panels: Panels, api: ServerAPI) = {
    tabsUI.add(
      Tab(
        tabData,
        buildHeader(tabData, onClicked = onClicked, onRemoved = onRemoved),
        content
      )
    )
    onAdded(tabData.safePath)
  }

  def tab(safePath: SafePath) = {
    tabsUI.tabs.now().filter { tab =>
      tab.t.safePath == safePath
    }.headOption
  }

  def tabData(safePath: SafePath) = tab(safePath).map(_.t)

  def editorPanelUI(safePath: SafePath) = tabData(safePath).flatMap(_.editorPanelUI)

  def removeTab(safePath: SafePath) = {
    tab(safePath).foreach { t =>
      tabsUI.remove(t.tabID)
    }
  }

  def alreadyDisplayed(safePath: SafePath) =
    tabsUI.tabs.now().find { t ⇒
      t.t.safePath.path == safePath.path
    }.map {
      _.tabID
    }

  def save(tabData: TabData, afterRefresh: TabData => Unit = _ => {}, overwrite: Boolean = false)(using panels: Panels, api: ServerAPI): Unit = {
    tabData.editorPanelUI.foreach { editorPanelUI =>
      editorPanelUI.synchronized {
        val (content, hash) = editorPanelUI.code
        api.saveFile(tabData.safePath, content, Some(hash), overwrite).foreach {
          case (saved, savedHash) ⇒
            if (saved) {
              editorPanelUI.onSaved(savedHash)
              afterRefresh(tabData)
            }
            else serverConflictAlert(tabData)
        }
      }
    }
  }

  def checkTabs(using api: ServerAPI) = tabsUI.tabs.now().foreach { tab =>
    api.exists(tab.t.safePath).foreach {
      e ⇒
        if (!e) removeTab(tab.t.safePath)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath) = {
    tabsUI.tabs.update {
      _.map { tab =>
        tab.copy(title = span(newSafePath.name), t = tab.t.copy(safePath = newSafePath))
      }
    }
  }

  def fontSizeLink(size: Int) = {
    div("A", fontSize := s"${
      size
    }px", cursor.pointer, padding := "3", onClick --> {
      _ ⇒
        for {
          ts ← tabsUI.tabs.now()
        } yield {
          ts.t.editorPanelUI.foreach(_.updateFont(size))
        }
    }
    )
  }

  //  def setErrors(path: SafePath, errors: Seq[ErrorWithLocation]) =
  //    find(path).foreach { tab ⇒ tab.editor.foreach { _.setErrors(errors) } }

  val fontSizeControl = div(cls := "file-content", display.flex, flexDirection.row, alignItems.baseline,
    fontSizeLink(15),
    fontSizeLink(25),
    fontSizeLink(35)
  )


