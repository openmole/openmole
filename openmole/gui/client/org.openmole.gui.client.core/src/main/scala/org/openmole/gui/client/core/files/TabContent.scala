package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.Panels
import org.openmole.gui.shared.data.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.NotificationManager.{Alternative, toService}
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import scaladget.tools.Utils.uuID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}

object TabContent:
  case class TabData(safePath: SafePath, editorPanelUI: Option[EditorPanelUI])

class TabContent:

  import TabContent.*

  val tabsUI = Tabs.tabs[TabData](Seq()).build
  val current: Var[Option[TabData]] = Var(None)

  def render(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    div(
      tabsUI.render.amend(margin := "10px"),
      fontSizeControl
    )

  private def buildHeader(
      tabData: TabData
  )(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    span(
      display.flex,
      flexDirection.row,
      alignItems.center,
      span(tabData.safePath.name),
      span(
        cls := "close-button close-button-tab bi-x",
        marginLeft := "5px",
        onClick --> { e =>
          save(tabData)
          removeTab(tabData.safePath)
          panels.treeNodePanel.clearErrorView(Some(tabData.safePath))
          panels.treeNodePanel.refresh
          e.stopPropagation()
        }
      )
    )

  def buildTab(
    tabData: TabData,
    content: HtmlElement,
    copyAttributes: Option[Tab[TabData]] = None
  )(using panels: Panels, api: ServerAPI, bp: BasePath) =

    val header =
      def switchPath = panels.treeNodePanel.treeNodeManager.switch(tabData.safePath.parent)
      buildHeader(tabData).amend(
        onClick --> { e =>
          if e.ctrlKey then switchPath
          current.set(Some(tabData))
        },
        onDblClick --> {_ => switchPath }
      )

    copyAttributes match
      case None => Tab(tabData, header, content)
      case Some(tab) =>
        Tab(
          tabData,
          header,
          content,
          tabID = tab.tabID,
          refID = tab.refID,
          active = tab.active
        )

  def addTab(tabData: TabData, content: HtmlElement)(using
      panels: Panels,
      api: ServerAPI,
      basePath: BasePath
  ) =
    tabsUI.add(buildTab(tabData, content))
    current.set(Some(tabData))

  def updateTab(safePath: SafePath, content: HtmlElement)(using
      panels: Panels,
      api: ServerAPI,
      basePath: BasePath
  ) =
    tabIndex(safePath).foreach: t=>
      val ind = tabsUI.tabs.now().indexOf(t._1)
      if (ind > -1)
      then
        val newTab = t._1.copy(content = content)
        tabsUI.tabs.update(tabs=>
          tabs.updated(ind, newTab)
        )

  def tab(safePath: SafePath) =
    tabsUI.tabs.now().find { tab => tab.t.safePath == safePath }

  def tabIndex(safePath: SafePath) =
    tabsUI.tabs.now().zipWithIndex.find { (tab, _) =>
      tab.t.safePath == safePath
    }

  def tabData(safePath: SafePath) = tab(safePath).map(_.t)

  def editorPanelUI(safePath: SafePath) =
    tabData(safePath).flatMap(_.editorPanelUI)

  def removeTab(safePath: SafePath) =
    tab(safePath).foreach { t => tabsUI.remove(t.tabID) }

  def alreadyDisplayed(safePath: SafePath) =
    tabsUI.tabs.now().find { t => t.t.safePath.path == safePath.path }.map {
      _.tabID
    }

  def save(
      tabData: TabData,
      overwrite: Boolean = false,
      saveUnmodified: Boolean = false
  )(using
      panels: Panels,
      api: ServerAPI,
      basePath: BasePath
  ): concurrent.Future[Boolean] = editorPanelUI.synchronized:
    import util.*
    tabData.editorPanelUI match
      case Some(editorPanelUI)
          if editorPanelUI.hasBeenModified || saveUnmodified =>
        val (content, hash) = editorPanelUI.code

        api.saveFile(tabData.safePath, content, Some(hash), overwrite).transform:
          case Failure(e) =>
            toService(panels.notifications).notifyError("Unable to save file", e, NotificationLevel.Info, None)
            Failure(e)
          case util.Success((saved, savedHash)) =>
            if saved
            then
              editorPanelUI.onSaved(savedHash)
              Success(true)
            else
              panels.notifications.showAlternativeNotification(
                NotificationLevel.Error,
                s"The file ${tabData.safePath.name} has been modified on the sever",
                div("Which version do you want to keep?"),
                Alternative(
                  "Yours",
                  _ =>
                    panels.tabContent
                      .save(tabData, overwrite = true, saveUnmodified = true)
                ),
                Alternative(
                  "Server",
                  _ =>
                    panels.treeNodePanel
                      .downloadFile(tabData.safePath, hash = true)
                      .map: (content: String, hash: Option[String]) =>
                        tabData.editorPanelUI
                          .foreach(_.setCode(content, hash.get))
                )
              )
              Success(false)
      case _ => concurrent.Future.successful(false)


  def closeNonExstingFiles(using api: ServerAPI, basePath: BasePath) =
    tabsUI.tabs
      .now()
      .foreach: tab =>
        api
          .exists(tab.t.safePath)
          .foreach: e =>
            if !e then removeTab(tab.t.safePath)

  def rename(
      sp: SafePath,
      newSafePath: SafePath
  )(using Panels, ServerAPI, GUIPlugins, BasePath) =
    tabIndex(sp).foreach: (ot, i) =>
      save(ot.t).foreach: _ =>
        FileDisplayer
          .buildTab(newSafePath)
          .foreach:
            case Some((nt, c)) =>
              tabsUI.tabs.update: tabs =>
                tabs.patch(i, Seq(buildTab(nt, c, Some(ot))), 1)
            case None =>

  def fontSizeLink(size: Int) =
    
    val selection = 
      tabsUI.tabs.now().headOption.flatMap: tab=>
        tab.t.editorPanelUI.map: ePUI=>
          ePUI.lineHeight.signal.map: lineHeight=>
            if(lineHeight == size) "fontSizeSelected"
            else ""

    div(
      "A",
      fontSize := s"${size}px",
      cursor.pointer,
      padding := "5",
      cls <-- selection.getOrElse(Var("")),
      onClick --> { _ =>
        for {
          ts ← tabsUI.tabs.now()
        } yield {
          ts.t.editorPanelUI.foreach(_.updateFont(size))
        }
      }
    )

  val fontSizeControl =
    div(
      position.absolute,
      right := "20",
      top := "62",
      cls := "file-content",
      display.flex,
      flexDirection.row,
      alignItems.baseline,
      children <-- tabsUI.tabs.signal.map(_.isEmpty match
        case true  => Seq(emptyNode)
        case false => Seq(fontSizeLink(17), fontSizeLink(25), fontSizeLink(35))
      )
    )
