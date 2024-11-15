package org.openmole.gui.client.core

import scala.scalajs.js.annotation.*
import org.scalajs.dom
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.scalajs.dom.{KeyboardEvent, document}

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.files.{FileDisplayer, TabContent, TreeNodeManager, TreeNodePanel}
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.client.ext.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.NotificationManager.toService
import org.openmole.gui.client.core.files.TreeNodePanel.MultiTool
import org.openmole.gui.client.ext.FileManager
import scaladget.bootstrapnative.bsn.*

import javax.swing.plaf.multi.MultiToolBarUI
import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.scalajs.js.timers.*

/*
 * Copyright (C) 15/04/15 // mathieu.leclaire@openmole.org
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

class OpenMOLEGUI(using panels: Panels, pluginServices: PluginServices, api: ServerAPI):

  def basePath =
    val basePath = dom.document.location.pathname.split("/").dropRight(1).mkString("/")
    BasePath(if basePath.isEmpty then None else Some(basePath))

  def stopped(): Unit =
    given BasePath = basePath

    val stoppedDiv = div(
      cls := "screen-center",
      img(src := "img/openmole_light.png", width := "600px"),
      div("The OpenMOLE server has been stopped", marginTop := "30px")
    )

    api.shutdown()
    render(dom.document.body, stoppedDiv)


  def restarted(): Unit =
    given BasePath = basePath

    val restartedDiv =
      div(
        cls := "screen-center",
        img(src := "img/openmole_light.png", width := "600px"),
        div("The OpenMOLE server is restarting, please wait.", marginTop := "30px")
      )

    api.restart()

    def checkAlive(): Unit =
      api.isAlive().foreach { x ⇒
        if x
        then CoreUtils.setRoute(s"/${connectionRoute}")
        else setTimeout(5000) {
          checkAlive()
        }
      }

    setTimeout(5000) {
      checkAlive()
    }

    render(dom.document.body, restartedDiv)


  def resetPassword(): Unit =
    val resetPassword = new ResetPassword
    render(
      dom.document.body,
      resetPassword.resetPassDiv
    )

  def run() =
    given BasePath = basePath

    val containerNode = dom.document.querySelector("#openmole-content")
    //import scala.concurrent.ExecutionContext.Implicits.global
    api.fetchGUIPlugins { plugins ⇒
      given GUIPlugins = plugins

      val authenticationPanel = AuthenticationPanel.render
      val newProjectPanel = ProjectPanel.render
      val settingsView = SettingsView.render

      val openFileTree = Var(true)

      dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
        if k.keyCode == 83 && k.ctrlKey then k.preventDefault()
        else if k.keyCode == 27
        then
          panels.treeNodePanel.multiTool.set(MultiTool.Off)
          panels.treeNodePanel.currentLine.set(-1)
          panels.closeExpandable
          panels.treeNodePanel.fileToolBar.filterToolOpen.set(false)
          panels.treeNodePanel.plusFile.set(false)
        }

      //START BUTTON
      lazy val theNavBar = div(
        cls := "nav-container",
        child <-- openFileTree.signal.map { oft ⇒
          div(
            navBarItem,
            if (oft) div(glyph_chevron_left) else div(glyph_chevron_right),
            onClick --> { _ ⇒
              openFileTree.update(!_)
            }
          )
        },
        div(row, justifyContent.flexStart, marginLeft := "20px",
          button(btn_danger, "New project",
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map:
              _.map { _.id }.contains(3)
            ,
            onClick --> { _ =>
              panels.expandTo(newProjectPanel, 3)
            }),
          div(OMTags.glyph_flash, navBarItem, marginLeft := "40px",
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map:
              _.map { _.id }.contains(4)
            ,
            onClick --> { _ ⇒
              ExecutionPanel.open
            }).tooltip("Executions"),
          div(glyph_lock, navBarItem,
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map:
              _.map { _.id }.contains(2)
            ,
            onClick --> { _ ⇒
              panels.expandTo(authenticationPanel, 2)
            }).tooltip("Authentications"),
          div(OMTags.glyph_plug, navBarItem,
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map:
              _.map { _.id }.contains(1)
            ,
            onClick --> { _ ⇒
              panels.pluginPanel.getPlugins
              panels.expandTo(panels.pluginPanel.render, 1)
            }).tooltip("Plugins"),
          div(OMTags.glyph_gear, navBarItem,
            cls.toggle("mainMenuCurrentGlyph") <-- panels.expandablePanel.signal.map:
              _.map { _.id }.contains(5)
            ,
            onClick --> { _ ⇒
              panels.expandTo(settingsView, 5)
            }).tooltip("Settings"),
          a(OMTags.glyph_info, cursor.pointer, navBarItem, target := "_blank", href <-- Signal.fromFuture(api.omSettings().map { sets ⇒
            s"https://${if (sets.isDevelopment) "next." else ""}openmole.org/Documentation.html"
          }).map { _.getOrElse("") }
          ).tooltip("Documentation"),
          div(child <-- panels.expandablePanel.signal.map(_.map(ep => Panels.ExpandablePanel.toString(ep.id)).getOrElse("")), cls := "mainMenuCurrentName")
        ),
        panels.notifications.render
      )

      def saveAllTabs = panels.tabContent.tabsUI.tabs.now().foreach { t => panels.tabContent.save(t.t) }

      def getServerNotifications =
        if !panels.notifications.isOpened
        then api.listNotification().foreach { n => panels.notifications.addServerNotifications(n) }

      render(
        containerNode,
        div(
          onKeyDown --> { k =>
            if k.keyCode == 83 && k.ctrlKey
            then saveAllTabs
          },
          EventStream.periodic(10000).toObservable --> Observer { _ => saveAllTabs },
          EventStream.periodic(10000).toObservable --> Observer { _ => getServerNotifications },
          cls := "app-container",
          div(
            cls := "main-container",
            div(
              cls <-- openFileTree.signal.map { oft ⇒
                "file-section" + {
                  if (oft) "" else " closed"
                }
              },
              div(img(src := "img/openmole_dark.png", height := "70px"), cls := "nav-container"),
              panels.treeNodePanel.fileControler,
              panels.treeNodePanel.fileToolBar.sortingGroup,
              panels.treeNodePanel.treeView
            ),
            div(
              cls := "tab-section",
              theNavBar,
              panels.tabContent.render
            )
          ),
          panels.notifications.notificationList,
          div(
            div(
              cls <-- panels.expandablePanel.signal.map: x =>
                "collapse-bottom " + {
                  x match
                    case Some(ep: Panels.ExpandablePanel) => ""
                    case _ => "close"
                },
              div(cls := "splitter"),
              child <-- panels.expandablePanel.signal.map: p ⇒
                p.map { _.element }.getOrElse(div(top := "1000px", color.white))
            )
          )
        )
      )
    }

    panels.treeNodePanel.refresh


@JSExportTopLevel(name = "openmole_library")
@JSExportAll
object App:
  lazy val panels = Panels()
  lazy val fetch = CoreFetch(panels)
  lazy val api = OpenMOLERESTServerAPI(fetch, toService(panels.notifications))

  lazy val pluginServices =
    PluginServices(
      errorManager = (message, stack) => panels.notifications.showGetItNotification(NotificationLevel.Error, message, div(stack))
    )

  val gui = OpenMOLEGUI(using panels, pluginServices, api)

  export gui.*

