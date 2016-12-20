package org.openmole.gui.client.core

import org.openmole.gui.client.core.panels._

import scalatags.JsDom.tags
import scala.scalajs.js.annotation.JSExport
import org.openmole.gui.client.tool.JsRxTags._
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
import org.openmole.gui.client.core.files.FileManager
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.tool.client.OMPost
import org.openmole.gui.ext.data.ProcessState
import org.openmole.gui.ext.data.Authentication
import org.scalajs
import org.scalajs.dom.raw.Event

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
  def connection(): Unit = {
    val connection = new Connection
    dom.document.body.appendChild(connection.connectionDiv.render)
  }

  @JSExport
  def stopped(): Unit = {
    dom.document.body.appendChild(
      div(omsheet.connectionTabOverlay)(
      div(
        img(src := "img/openmole.png", omsheet.openmoleLogo),
        div(
          omsheet.centerPage,
          div(omsheet.shutdown, "The OpenMOLE server has been stopped"),
          onload := { () ⇒ OMPost()[Api].shutdown().call() }
        )
      )
    ).render
    )
  }

  @JSExport
  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    dom.document.body.appendChild(
      resetPassword.resetPassDiv.render
    )
  }

  @JSExport
  def run(): Unit = {
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

    val maindiv = tags.div()
    val shutDown = new ShutDown

    val authenticationPanel = new AuthenticationPanel

    val openFileTree = Var(true)

    val itemStyle = lineHeight := "35px"

    val execItem = navItem(tags.div(glyph_flash, itemStyle).tooltip("Executions"), () ⇒ executionPanel.dialog.open)

    val authenticationItem = navItem(tags.div(glyph_lock, itemStyle).tooltip("Authentications"), () ⇒ authenticationPanel.dialog.open)

    val marketItem = navItem(tags.div(glyph_market, itemStyle).tooltip("Market place"), () ⇒ marketPanel.dialog.open)

    val pluginItem = navItem(div(OMTags.glyph_plug, itemStyle).tooltip("Plugins"), () ⇒ pluginPanel.dialog.open)

    val envItem = navItem(div(glyph_exclamation, itemStyle), () ⇒ stackPanel.open)

    val docItem = navItem(div(OMTags.glyph_book, itemStyle).tooltip("Documentation"), () ⇒ docPanel.dialog.open)

    val modelWizardItem = navItem(div(glyph_upload_alt, itemStyle).tooltip("Model import"), () ⇒ modelWizardPanel.dialog.open)

    val fileItem = navItem(div(glyph_file, itemStyle).tooltip("Files"), todo = () ⇒ {
      openFileTree() = !openFileTree.now
    })

    dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
      if ((k.keyCode == 83 && k.ctrlKey)) {
        k.preventDefault
        false

      }
    }

    Settings.settings.map { sets ⇒
      dom.document.body.appendChild(
        div(
        ///////Plugin test
        div(
          bs.button("build", btn_danger +++ ms("ooo"), () ⇒ Plugins.load),
          bs.button("call", btn_primary +++ ms("oooo"), () ⇒ {
            OMPost()[Api].getGUIPlugins.call().foreach { all ⇒
              Plugins.authentications() = all.authentications.map { gp ⇒ Plugins.buildJSObject(gp.jsObject).asInstanceOf[Authentication] }

              //TEst
              println("auth:" + Plugins.authentications.now)

              val oo = Plugins.authentications.now.headOption.map { h ⇒
                println("h " + h)
                // println("h " + h.test)
                // println("h " + h.panel)
                h.panel
              }.getOrElse(tags.div("Cannot load"))

              println("OO " + oo)

              org.scalajs.dom.document.body.appendChild(oo.render)
              // Plugins.authentications.now.headOption.map { _.test }
            }

          })
        ),
        //////
        bs.navBar(
          omsheet.fixed +++ sheet.nav +++ navbar_pills +++ navbar_inverse +++ navbar_staticTop,
          fileItem,
          modelWizardItem,
          execItem,
          authenticationItem,
          marketItem,
          pluginItem,
          docItem
        ),
        tags.div(shutDown.shutdownButton),
        tags.div(`class` := "fullpanel")(
          tags.div(
            `class` := Rx {
              "leftpanel " + {
                if (openFileTree()) "open" else ""
              }
            }
          )(
              tags.div(omsheet.fixedPosition)(
                treeNodePanel.fileToolBar.div,
                treeNodePanel.fileControler,
                treeNodePanel.labelArea
              ),
              treeNodePanel.view.render
            ),
          tags.div(
            `class` := Rx {
              "centerpanel " + {
                if (openFileTree()) "reduce" else ""
              }
            }
          )(
              treeNodePanel.fileDisplayer.tabs.render,
              tags.div(omsheet.textVersion)(
                tags.div(
                  fontSize := "1em",
                  fontWeight := "bold"
                )(s"${sets.version} ${sets.versionName}"),
                tags.div(fontSize := "0.8em")(s"built the ${sets.buildTime}")
              )
            )
        )
      ).render
      )
    }

    body.appendChild(maindiv)

  }
}