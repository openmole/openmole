package org.openmole.gui.client.core

import org.openmole.gui.client.core.panels._

import scala.scalajs.js.annotation._
import org.scalajs.dom
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.scalajs.dom.KeyboardEvent

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.alert.{ AlertPanel, BannerAlert }
import org.openmole.gui.client.core.files.{ TreeNodePanel }
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client.FileManager
import org.openmole.gui.ext.client._
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

import scala.concurrent.duration._
import scala.scalajs.js.timers._

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

@JSExportTopLevel(name = "openmole_client") @JSExportAll
object App {

  def connection(): Unit = {
    render(
      dom.document.body,
      div(
        panels.connection.render,
        panels.alertPanel.alertDiv
      )
    )
  }

  def stopped(): Unit = {

    val stoppedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        div(
          omsheet.centerPage(),
          div(omsheet.shutdown, "The OpenMOLE server has been stopped")
        )
      )
    )

    Post()[Api].shutdown().call()
    render(dom.document.body, stoppedDiv)
  }

  def restarted(): Unit = {
    val timer: Var[Option[SetIntervalHandle]] = Var(None)

    def setTimer = {
      timer.set(Some(setInterval(5000) {
        Post(3 seconds, 5 minutes)[Api].isAlive().call().foreach { x ⇒
          if (x) {
            CoreUtils.setRoute(routes.connectionRoute)
            timer.now.foreach {
              clearInterval
            }
          }
        }
      })
      )
    }

    setTimer
    val restartedDiv = div(
      omsheet.connectionTabOverlay,
      div(
        omsheet.centerPage(),
        div(omsheet.shutdown, "The OpenMOLE server is restarting, please wait.")
      )
    )

    Post()[Api].restart().call()
    render(dom.document.body, restartedDiv)
  }

  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    render(
      dom.document.body,
      div(
        resetPassword.resetPassDiv,
        panels.alertPanel.alertDiv
      )
    )
  }

  def run(): Unit = {

    Plugins.fetch { plugins ⇒
      val maindiv = div()

      val authenticationPanel = new AuthenticationPanel(plugins.authenticationFactories)

      val openFileTree = Var(true)

      val itemStyle = lineHeight := "35px"

      val execItem = navItem(div(OMTags.glyph_flash, itemStyle).tooltip("Executions"), () ⇒ openExecutionPanel)

      val authenticationItem = navItem(div(glyph_lock, itemStyle).tooltip("Authentications"), () ⇒ authenticationPanel.authenticationDialog.show)

      val pluginItem = navItem(div(OMTags.glyph_plug, itemStyle).tooltip("Plugins"), () ⇒ pluginPanel.pluginDialog.show)

      val envItem = navItem(div(glyph_exclamation, itemStyle), () ⇒ stackPanel.open)

      val settingsItem = navItem(div(
        //panels.settingsViewApp,
        itemStyle), () ⇒ {}).right

      val actionItem = navItem(div(
        child <-- treeNodeTabs.temporaryControl.signal
      ))

      dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
        if (k.keyCode == 83 && k.ctrlKey) {
          k.preventDefault
          false
        }
      }

      case class MenuAction(name: String, action: () ⇒ Unit)

      lazy val newEmpty = MenuAction("Empty project", () ⇒ {
        val fileName = "newProject.oms"
        CoreUtils.addFile(panels.treeNodeManager.current.now, fileName, () ⇒ {
          val toDisplay = panels.treeNodeManager.current.now ++ fileName
          FileManager.download(
            toDisplay,
            hash = true,
            onLoaded = (content, hash) ⇒ {
              panels.treeNodePanel.refreshAndDraw
              fileDisplayer.display(toDisplay, content, hash.get, FileExtension.OMS, panels.pluginServices)
            }
          )
        })
      })

      //START BUTTON
      lazy val theNavBar = div(
        child <-- openFileTree.signal.map { oft ⇒
          navBar(
            Seq(
              navbar_inverse,
              omsheet.absoluteFullWidth, fontSize := "20",
              if (oft) mainNav370 else mainNav0
            ),
            navItem(
              if (oft) div(glyph_chevron_left) else div(glyph_chevron_right),
              todo = () ⇒ {
                openFileTree.update(!_)
              }
            ),
            navItem(menuActions.selector),
            execItem,
            authenticationItem,
            pluginItem,
            actionItem,
            settingsItem
          ).render
        }
      )

      lazy val importModel = MenuAction("Import your model", () ⇒ {
        modelWizardPanel(plugins.wizardFactories).dialog.show
      })

      lazy val fromURLProject = MenuAction("From URL", () ⇒ {
        urlImportPanel.urlDialog.show
      })

      lazy val marketPlaceProject = MenuAction("From market place", () ⇒ {
        marketPanel.modalDialog.show
      })

      lazy val elements = Seq(newEmpty, importModel, marketPlaceProject, fromURLProject)

      lazy val menuActions: Options[MenuAction] = elements.options(
        key = btn_danger,
        naming = (m: MenuAction) ⇒ m.name,
        onclose = () ⇒ menuActions.content.now.foreach {
          _.action()
        },
        fixedTitle = Some("New project")
      )

      // Define the option sequence

      Settings.settings.map { sets ⇒
        dom.document.body.appendChild(
          div(
            div(
              cls := "fullpanel",
              panels.bannerAlert.banner,
              theNavBar,
              div(
                cls <-- openFileTree.signal.map { oft ⇒ "leftpanel" + CoreUtils.ifOrNothing(oft, "open") },
                div(omsheet.relativePosition, paddingTop := "-15",
                  treeNodePanel.fileToolBar.element,
                  treeNodePanel.fileControler,
                  treeNodePanel.labelArea,
                  treeNodePanel.view
                )
              ),
              div(
                cls <-- openFileTree.signal.combineWith(panels.bannerAlert.isOpen).map {
                  case (oft, io) ⇒
                    "centerpanel " +
                      CoreUtils.ifOrNothing(oft, "reduce") +
                      CoreUtils.ifOrNothing(io, " banneropen")
                },
                treeNodeTabs.render,
                div(
                  omsheet.textVersion,
                  div(
                    fontSize := "1em", s"${sets.version} ${sets.versionName}"),
                  div(fontSize := "0.8em", s"built the ${sets.buildTime}")
                )
              )
            ),
            panels.alertPanel.alertDiv
          ).ref
        )
      }
    }
  }

}