package org.openmole.gui.client.core

import org.openmole.gui.client.core.panels._

import scalatags.JsDom.tags
import scala.scalajs.js.annotation.JSExport
import org.openmole.gui.ext.tool.client.JsRxTags._
import org.scalajs.dom
import rx._

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
import bs._
import org.openmole.gui.client.tool._
import org.scalajs.dom.KeyboardEvent

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.api.Selector.Options
import org.openmole.gui.client.core.alert.{ AlertPanel, BannerAlert }
import org.openmole.gui.client.core.files.{ FileDisplayer, TreeNodePanel }
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.openmole.gui.ext.tool.client._

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

@JSExport("ScriptClient")
object ScriptClient {

  @JSExport
  def connection(): Unit = withBootstrapNative {
    val connection = new Connection
    div(
      connection.connectionDiv.render,
      alert
    ).render
  }

  @JSExport
  def stopped(): Unit = withBootstrapNative {
    div(omsheet.connectionTabOverlay)(
      div(
        img(src := "img/openmole.png", omsheet.openmoleLogo),
        div(
          omsheet.centerPage,
          div(omsheet.shutdown, "The OpenMOLE server has been stopped"),
          onload := { () ⇒ post()[Api].shutdown().call() }
        )
      )
    ).render
  }

  @JSExport
  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    withBootstrapNative {
      div(
        resetPassword.resetPassDiv,
        alert
      ).render
    }
  }

  @JSExport
  def run(): Unit = {
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

    post()[Api].loadPlugins.call().map { _ ⇒
      val maindiv = tags.div()
      val shutDown = new ShutDown

      val authenticationPanel = new AuthenticationPanel

      val openFileTree = Var(true)

      val itemStyle = lineHeight := "35px"

      val execItem = navItem(tags.div(glyph_flash, itemStyle).tooltip("Executions"), () ⇒ executionPanel.dialog.show)

      val authenticationItem = navItem(tags.div(glyph_lock, itemStyle).tooltip("Authentications"), () ⇒ authenticationPanel.dialog.show)

      val pluginItem = navItem(div(OMTags.glyph_plug, itemStyle).tooltip("Plugins"), () ⇒ pluginPanel.dialog.show)

      val envItem = navItem(div(glyph_exclamation, itemStyle), () ⇒ stackPanel.open)

      val docItem = navItem(div(OMTags.glyph_book, itemStyle).tooltip("Documentation"), () ⇒ docPanel.dialog.show)

      dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
        if ((k.keyCode == 83 && k.ctrlKey)) {
          k.preventDefault
          false

        }
      }

      //START BUTTON
      val fileDisplayer = new FileDisplayer

      // Define the option sequence
      case class MenuAction(name: String, action: () ⇒ Unit)

      val newEmpty = MenuAction("Empty project", () ⇒ {
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

      val importModel = MenuAction("Import your model", () ⇒ {
        modelWizardPanel.dialog.show
      })

      val marketPlaceProject = MenuAction("From market place", () ⇒ {
        marketPanel.dialog.show
      })

      val elements = Seq(newEmpty, importModel, marketPlaceProject)

      lazy val menuActions: Options[MenuAction] = elements.options(
        key = btn_danger,
        naming = (m: MenuAction) ⇒ m.name,
        onclose = () ⇒ menuActions.content.now.foreach {
        _.action()
      },
        fixedTitle = Some("New project")
      )

      Settings.settings.map { sets ⇒

        withBootstrapNative {
          div(
            ///////Plugin test
            div(
              bs.button("build", btn_danger +++ ms("ooo"), () ⇒ Plugins.buildAndLoad)
            ),
            //////
            Rx {
              bs.navBar(
                omsheet.fixed +++ sheet.nav +++ navbar_pills +++ navbar_inverse +++ (fontSize := 20) +++ navbar_staticTop +++ {
                  if (openFileTree()) mainNav370 else mainNav0
                },
                navItem(
                  if (openFileTree()) div(glyph_chevron_left, fileChevronStyle) else div(glyph_chevron_right, fileChevronStyle),
                  todo = () ⇒ {
                    openFileTree() = !openFileTree.now
                  }
                ),
                navItem(menuActions.selector),
                execItem,
                authenticationItem,
                pluginItem,
                docItem
              )
            },
            tags.div(shutDown.shutdownButton),
            tags.div(`class` := "fullpanel")(
              tags.div(
                `class` := Rx {
                  "leftpanel " + {
                    if (openFileTree()) "open" else ""
                  }
                }
              )(
                  tags.div(omsheet.relativePosition +++ sheet.paddingTop(-15))(
                    treeNodePanel.fileToolBar.div,
                    treeNodePanel.fileControler,
                    treeNodePanel.labelArea,
                    treeNodePanel.view.render
                  )
                ),
              tags.div(
                `class` := Rx {
                  "centerpanel " + {
                    if (openFileTree()) "reduce" else ""
                  }
                }
              )(
                  BannerAlert.banner,
                  treeNodePanel.fileDisplayer.tabs.render,
                  tags.div(omsheet.textVersion)(
                    tags.div(
                      fontSize := "1em",
                      fontWeight := "bold"
                    )(s"${sets.version} ${sets.versionName}"),
                    tags.div(fontSize := "0.8em")(s"built the ${sets.buildTime}")
                  )
                )
            ),
            alert
          ).render
        }
      }
    }
  }

  def alert = AlertPanel.alertDiv
}