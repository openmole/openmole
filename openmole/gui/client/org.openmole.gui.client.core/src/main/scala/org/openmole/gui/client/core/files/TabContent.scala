package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.Panels
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.NotificationManager.{Alternative, toService}
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.timers.{SetIntervalHandle, clearInterval, setInterval}
import io.github.nguyenyou.webawesome.laminar.*

object TabContent:
  case class TabData(safePath: SafePath, editorPanelUI: Option[EditorPanelUI], controlElement: Option[HtmlElement] = None)

class TabContent:

  import TabContent.*

  val tabs: Var[Seq[TabData]] = Var(Seq())
  val activeTab: Var[Option[TabData]] = Var(None)

  case class NavEntry(tabData: TabData, isClose: Boolean)

  def navEntries(ts: Seq[TabData]): Seq[NavEntry] =
    ts.flatMap(td => Seq(
      NavEntry(td, isClose = false),
      NavEntry(td, isClose = true)
    ))

  def closeButton(tabData: TabData) =
    Button(
      _.slot := "nav",
      _.tabIndex := -1,
      _.appearance.plain,
      _.size := "s",
      _.onClick --> { _ =>
        val newTabs =
          tabs.now().filterNot(_.safePath == tabData.safePath)

        tabs.set(newTabs)

        activeTab.update {
          case Some(active) if active.safePath == tabData.safePath => newTabs.lastOption
          case None => newTabs.lastOption
          case Some(active) => Some(active)
        }
      }
    )(
      Icon(
        _.name := "xmark",
        _.label := "Close this tab"
      )()
    )

  lazy val tabGroup =

    def switchPath(tabData: TabData) = Panels().treeNodePanel.treeNodeManager.switch(tabData.safePath.parent)

    def build(tabData: TabData) = {
      Tab(
        _.panel := tabData.safePath.name,
        _.onClick --> { e =>
          if e.ctrlKey then switchPath(tabData)
          activeTab.set(Some(tabData))
        },
        _.onDblClick --> { _ =>
          switchPath(tabData)
        }
      )(tabData.safePath.name)
    }


    TabGroup(
      _.activation.auto,
      _.cls := "tabs-closable",
      _.slots.nav(
        children <-- tabs.signal
          .map(navEntries)
          .split(entry => (entry.tabData.safePath, entry.isClose)) {
            (_, entry, _) =>
              if entry.isClose then closeButton(entry.tabData)
              else build(entry.tabData)
          }
      ),
      _.active <-- activeTab.signal.map(x =>
        x.map(_.safePath.name).getOrElse("")
      )
    )(
      children <-- tabs.signal.split(_.safePath) { (_, tabData, _) =>
        TabPanel(
          _.name := tabData.safePath.name
        )(
          tabData.editorPanelUI
            .map(_.view)
            .getOrElse(div())
        )
      }
    )


  val current: Var[Option[TabData]] = Var(None)
  val controlElement: Var[HtmlElement] = Var(div())

  def setActive(tabData: TabData) =
    activeTab.set(Some(tabData))

  def buildTab(tabData: TabData) = tabData


  def addTab(tabData: TabData) =
    tabs.update(_ :+ tabData)
    activeTab.set(Some(tabData))

  def updateTab(safePath: SafePath, content: HtmlElement)(using
                                                          panels: Panels,
                                                          api: ServerAPI,
                                                          basePath: BasePath) =
    println("FIXME")
  //    tabIndex(safePath).foreach: ind =>
  //      tabs.update(ts =>
  //        ts.updated(ind, ts(ind).editorPanelUI.map(epu => EditorPanelUI(safePath, content, epu.contentHash)))
  //      )

  def tab(safePath: SafePath) =
    tabs.now().find { tab => tab.safePath == safePath }

  def tabIndex(safePath: SafePath) =
    val ind = tabs.now().indexWhere(t => t.safePath == safePath)
    if ind > -1
    then Some(ind)
    else None

  def editorPanelUI(safePath: SafePath) =
    tab(safePath).flatMap(_.editorPanelUI)

  def removeTab(safePath: SafePath) = //{
    tabs.now().filterNot(_.safePath == safePath)

  def alreadyDisplayed(safePath: SafePath) =
    tabs.now().find(_.safePath == safePath)

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

        api.saveFile(tabData.safePath, content, if overwrite then None else Some(hash)).transform:
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
    tabs.now().foreach: tab =>
      api.exists(tab.safePath).foreach: e =>
        if !e then removeTab(tab.safePath)

  def rename(
              sp: SafePath,
              newSafePath: SafePath
            )(using Panels, ServerAPI, GUIPlugins, BasePath, PluginServices, NotificationService) = {
    tabIndex(sp).foreach: ind =>
      tabs.update: ts =>
        ts.updated(ind, ts(ind).copy(safePath = newSafePath))
      activeTab.set(tab(sp))
  }

  def fontSizeLink(size: Int) =

    val selection =
      tabs.now().headOption.flatMap: tab =>
        tab.editorPanelUI.map: ePUI =>
          ePUI.lineHeight.signal.map: lineHeight =>
            if (lineHeight == size) "fontSizeSelected"
            else ""

    div(
      "A",
      fontSize := s"${size}px",
      cursor.pointer,
      padding := "5",
      cls <-- selection.getOrElse(Var("")),
      onClick --> { _ =>
        for {
          ts ← tabs.now()
        } yield {
          ts.editorPanelUI.foreach(_.updateFont(size))
        }
      }
    )

  val fontSizeControl =
    div(
      display.flex,
      alignItems.baseline,
      position.absolute,
      top := "10",
      right := "800",
      color.white,
      children <-- tabs.signal.map(_.isEmpty match
        case true => Seq(emptyNode)
        case false => Seq(fontSizeLink(17), fontSizeLink(25), fontSizeLink(35))
      )
    )
