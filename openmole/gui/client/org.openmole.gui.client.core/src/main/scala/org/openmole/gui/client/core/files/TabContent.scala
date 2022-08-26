package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{Post, panels}
import org.openmole.gui.ext.data._
import scaladget.bootstrapnative.bsn._
import com.raquo.laminar.api.L._
import org.openmole.gui.client.core.files.TreeNodeTab.{save, serverConflictAlert}
import org.openmole.gui.ext.api.Api
import scaladget.tools.Utils.uuID
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._

import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}


object TabContent {

  case class TabData(safePath: SafePath, editorPanelUI: Option[EditorPanelUI])

  val tabsUI = Tabs.tabs[TabData](Seq()).build

  val timer: Var[Option[SetIntervalHandle]] = Var(None)

  val tabsObserver = Observer[Seq[Tab[TabData]]] { tabs =>
    if (tabs.isEmpty) {
      timer.now.foreach { handle =>
        clearInterval(handle)
      }
      timer.set(None)
    }
  }

  val timerObserver = Observer[Option[SetIntervalHandle]] { handle =>
    handle match {
      case None => timer.set(Some(setInterval(15000) {
        TabContent.tabsUI.tabs.now.foreach { t =>
          save(t.t)
        }
      }))
      case _ =>
    }
  }

  val render =
    tabsUI.render.amend(
      margin := "10px",
      tabsUI.tabs --> tabsObserver,
      timer --> timerObserver
    )

  private def buildHeader(tabData: TabData, onRemoved: SafePath => Unit, onClicked: SafePath => Unit) = {
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

  def addTab(tabData: TabData,
             content: HtmlElement,
             onClicked: SafePath => Unit = _ => {},
             onAdded: SafePath => Unit = _ => {},
             onRemoved: SafePath => Unit = _ => {}
            ) = {
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

  def removeTab(safePath: SafePath) = {
    tab(safePath).foreach { t =>
      tabsUI.remove(t.tabID)
    }
  }

  def save(tabData: TabData, afterRefresh: TabData => Unit = _ => {}, overwrite: Boolean = false): Unit = {
    tabData.editorPanelUI.foreach { editorPanelUI =>
      editorPanelUI.synchronized {
        val (content, hash) = editorPanelUI.code
        Post()[Api].saveFile(tabData.safePath, content, Some(hash), overwrite).call().foreach {
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

  def checkTabs = tabsUI.tabs.now.foreach { tab =>
    org.openmole.gui.client.core.Post()[Api].exists(tab.t.safePath).call().foreach {
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
          ts ← tabsUI.tabs.now
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
}
