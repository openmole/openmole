package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.{CoreFetch, Panels}
import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.NotificationManager.Alternative
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import scaladget.tools.Utils.uuID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}


object TabContent:
  case class TabData(safePath: SafePath, editorPanelUI: Option[EditorPanelUI])

class TabContent:

  import TabContent.TabData

  val tabsUI = Tabs.tabs[TabData](Seq()).build

  def render(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    tabsUI.render.amend(margin := "10px")

  private def buildHeader(tabData: TabData)(using panels: Panels, api: ServerAPI, basePath: BasePath) = {
    span(display.flex, flexDirection.row, alignItems.center,
      span(tabData.safePath.name),
      span(cls := "close-button close-button-tab bi-x", marginLeft := "5px", onClick --> { e =>
        save(tabData)
        removeTab(tabData.safePath)
        e.stopPropagation()
      })
    )
  }

  def addTab(
    tabData: TabData,
    content: HtmlElement)(using panels: Panels, api: ServerAPI, basePath: BasePath) = {
    tabsUI.add(
      Tab(
        tabData,
        buildHeader(tabData),
        content
      )
    )
  }

  def tab(safePath: SafePath) =
    tabsUI.tabs.now().filter { tab => tab.t.safePath == safePath }.headOption

  def tabData(safePath: SafePath) = tab(safePath).map(_.t)

  def editorPanelUI(safePath: SafePath) = tabData(safePath).flatMap(_.editorPanelUI)

  def removeTab(safePath: SafePath) = 
    tab(safePath).foreach { t => tabsUI.remove(t.tabID) }
  
  def alreadyDisplayed(safePath: SafePath) =
    tabsUI.tabs.now().find { t ⇒ t.t.safePath.path == safePath.path }.map { _.tabID }

  def save(tabData: TabData, overwrite: Boolean = false, saveUnmodified: Boolean = false)(using panels: Panels, api: ServerAPI, basePath: BasePath): concurrent.Future[Boolean] = editorPanelUI.synchronized {
    tabData.editorPanelUI match
      case Some(editorPanelUI) if editorPanelUI.hasBeenModified || saveUnmodified =>
        val (content, hash) = editorPanelUI.code
        api.saveFile(tabData.safePath, content, Some(hash), overwrite).map {
          case (saved, savedHash) ⇒
            if saved
            then
              editorPanelUI.onSaved(savedHash)
              true
            else
              panels.notifications.showAlternativeNotification(
                NotificationLevel.Error,
                s"The file ${tabData.safePath.name} has been modified on the sever",
                div("Which version do you want to keep?"),
                Alternative("Yours", _ ⇒ panels.tabContent.save(tabData, overwrite = true, saveUnmodified = true)),
                Alternative("Server", _ =>
                  panels.treeNodePanel.downloadFile(tabData.safePath, hash = true).map { (content: String, hash: Option[String]) ⇒
                    tabData.editorPanelUI.foreach(_.setCode(content, hash.get))
                  }
                )
              )
              false
        }
      case _ => concurrent.Future.successful(false)
  }


  def checkTabs(using api: ServerAPI, basePath: BasePath) = tabsUI.tabs.now().foreach { tab =>
    api.exists(tab.t.safePath).foreach {
      e ⇒
        if (!e) removeTab(tab.t.safePath)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath)(using panels: Panels, api: ServerAPI, basePath: BasePath) = {
    tabsUI.tabs.update { ts =>
      ts.map { tab =>
        if tab.t.safePath == sp
        then
          val newT = tab.t.copy(safePath = newSafePath)
          tab.copy(t = newT, title = buildHeader(newT))
        else tab
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

  val fontSizeControl = div(cls := "file-content", display.flex, flexDirection.row, alignItems.baseline,
    fontSizeLink(17),
    fontSizeLink(25),
    fontSizeLink(35)
  )


