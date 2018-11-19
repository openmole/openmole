package org.openmole.gui.client.core

import org.openmole.gui.client.core.panels._

import scala.scalajs.js.annotation._
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.scalajs.dom.KeyboardEvent

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._

import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.alert.{ AlertPanel, BannerAlert }
import org.openmole.gui.client.core.files.TreeNodePanel
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.openmole.gui.ext.tool.client._
import org.scalajs.dom.raw.HTMLDivElement

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

object ScriptClient {

  @JSExportTopLevel("connection")
  def connection(): Unit =
    dom.document.body.appendChild(
      div(
        Connection.render,
        alert
      ).render
    )

  @JSExportTopLevel("stopped")
  def stopped(): Unit = {

    val stoppedDiv = div(omsheet.connectionTabOverlay)(
      div(
        div(
          omsheet.centerPage(),
          div(omsheet.shutdown, "The OpenMOLE server has been stopped")
        )
      )
    ).render

    post()[Api].shutdown().call()
    dom.document.body.appendChild(stoppedDiv)
  }

  @JSExportTopLevel("restarted")
  def restarted(): Unit = {
    val timer: Var[Option[SetIntervalHandle]] = Var(None)

    def setTimer = {
      timer() = Some(setInterval(5000) {
        post(3 seconds, 5 minutes)[Api].isAlive().call().foreach { x ⇒
          if (x) {
            CoreUtils.setRoute(routes.connectionRoute)
            timer.now.foreach {
              clearInterval
            }
          }
        }
      })
    }

    setTimer
    val restartedDiv = div(omsheet.connectionTabOverlay)(
      div(
        div(
          omsheet.centerPage(),
          div(omsheet.shutdown, "The OpenMOLE server is restarting, please wait.")
        )
      )
    ).render

    post()[Api].restart().call()
    dom.document.body.appendChild(restartedDiv)
  }

  @JSExportTopLevel("resetPassword")
  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    dom.document.body.appendChild(
      div(
        resetPassword.resetPassDiv,
        alert
      ).render
    )
  }

  @JSExportTopLevel("run")
  def run(): Unit = {
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

    Plugins.fetch { _ ⇒
      val maindiv = div()
      //  val settingsView = new SettingsView

      val authenticationPanel = new AuthenticationPanel

      val openFileTree = Var(true)

      val itemStyle = lineHeight := "35px"

      val execItem = navItem(div(glyph_flash, itemStyle).tooltip("Executions"), () ⇒ executionPanel.dialog.show)

      val authenticationItem = navItem(div(glyph_lock, itemStyle).tooltip("Authentications"), () ⇒ authenticationPanel.dialog.show)

      val pluginItem = navItem(div(OMTags.glyph_plug, itemStyle).tooltip("Plugins"), () ⇒ pluginPanel.dialog.show)

      val envItem = navItem(div(glyph_exclamation, itemStyle).render, () ⇒ stackPanel.open)

      val settingsItem = navItem(div(SettingsView.renderApp, itemStyle).render, () ⇒ {}).right

      val actionItem = navItem(div(
        Rx {
          treeNodeTabs.temporaryControl()
        }).render)

      dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
        if ((k.keyCode == 83 && k.ctrlKey)) {
          k.preventDefault
          false

        }
      }

      case class MenuAction(name: String, action: () ⇒ Unit)

      lazy val newEmpty = MenuAction("Empty project", () ⇒ {
        val fileName = "newProject.oms"
        CoreUtils.addFile(manager.current.now, fileName, () ⇒ {
          val toDisplay = manager.current.now ++ fileName
          FileManager.download(
            toDisplay,
            onLoadEnded = (content: String) ⇒ {
              TreeNodePanel.refreshAndDraw
              fileDisplayer.display(toDisplay, content, FileExtension.OMS)
            }
          )
        })
      })

      //START BUTTON
      lazy val theNavBar = div(
        Rx {
          navBar(
            omsheet.absoluteFullWidth +++ nav +++ navbar_pills +++ navbar_inverse +++ (fontSize := 20) +++ navbar_staticTop +++ {
              if (openFileTree()) mainNav370 else mainNav0
            },
            navItem(
              if (openFileTree()) div(glyph_chevron_left).render else div(glyph_chevron_right).render,
              todo = () ⇒ {
                openFileTree() = !openFileTree.now
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
        modelWizardPanel.dialog.show
      })

      lazy val cloneRepositiory = MenuAction("Clone repository", () ⇒ {
        versioningPanel.dialog.show
      })

      lazy val marketPlaceProject = MenuAction("From market place", () ⇒ {
        marketPanel.dialog.show
      })

      lazy val elements = Seq(newEmpty, importModel, marketPlaceProject, cloneRepositiory)

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
            div(`class` := "fullpanel")(
              BannerAlert.banner,
              theNavBar,
              div(
                `class` := Rx {
                  "leftpanel " + {
                    CoreUtils.ifOrNothing(openFileTree(), "open")
                  }
                }
              )(
                  div(omsheet.relativePosition +++ (paddingTop := -15))(
                    treeNodePanel.fileToolBar.div,
                    treeNodePanel.fileControler,
                    treeNodePanel.labelArea,
                    treeNodePanel.view
                  )
                ),
              div(
                `class` := Rx {
                  "centerpanel " +
                    CoreUtils.ifOrNothing(openFileTree(), "reduce") +
                    CoreUtils.ifOrNothing(BannerAlert.isOpen(), " banneropen")
                }
              )(
                  treeNodeTabs.render,
                  div(omsheet.textVersion)(
                    div(
                      fontSize := "1em"
                    )(s"${sets.version} ${sets.versionName}"),
                    div(fontSize := "0.8em")(s"built the ${sets.buildTime}")
                  )
                )
            ),
            alert
          ).render
        )
      }
    }
  }

  def alert = AlertPanel.alertDiv
}