package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.AlertPanel
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
import org.openmole.gui.client.tool.{ OMPost, OMTags }
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.ProcessState
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

    dom.document.body.appendChild(connection.connectionDiv)
    dom.document.body.appendChild(AlertPanel.alertDiv)
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
      )
    )
  }

  @JSExport
  def resetPassword(): Unit = {
    val resetPassword = new ResetPassword
    dom.document.body.appendChild(resetPassword.resetPassDiv)
  }

  @JSExport
  def run(): Unit = {

    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

    val body = dom.document.body
    val maindiv = body.appendChild(tags.div())
    val shutDown = new ShutDown

    val authenticationPanel = new AuthenticationPanel

    val openFileTree = Var(true)

    val authenticationTriggerer = new PanelTriggerer {
      val modalPanel = authenticationPanel
    }

    val execItem = dialogNavItem("executions", glyphSpan(glyph_flash).tooltip(span("Executions")), () ⇒ executionTriggerer.triggerOpen)

    val authenticationItem = dialogNavItem("authentications", glyphSpan(glyph_lock).tooltip(span("Authentications")), () ⇒ authenticationTriggerer.triggerOpen)

    val marketItem = dialogNavItem("market", glyphSpan(glyph_market).tooltip(span("Market place")), () ⇒ marketTriggerer.triggerOpen)

    val pluginItem = dialogNavItem("plugin", div(OMTags.glyph_plug).tooltip(span("Plugins")), () ⇒ pluginTriggerer.triggerOpen)

    val envItem = dialogNavItem("envError", glyphSpan(glyph_exclamation).render, () ⇒ environmentStackTriggerer.open)

    val docItem = dialogNavItem("doc", div(OMTags.glyph_book).tooltip(span("Documentation")), () ⇒ docTriggerer.open)

    val modelWizardItem = dialogNavItem("modelWizard", glyphSpan(glyph_upload_alt).tooltip(span("Model import")), () ⇒ modelWizardTriggerer.triggerOpen)

    val fileItem = dialogNavItem("files", glyphSpan(glyph_file).tooltip(span("Files")), todo = () ⇒ {
      openFileTree() = !openFileTree.now
    })

    dom.window.onkeydown = (k: KeyboardEvent) ⇒ {
      if ((k.keyCode == 83 && k.ctrlKey)) {
        k.preventDefault
        false

      }
    }

    maindiv.appendChild(
      bs.nav(
        "mainNav",
        omsheet.fixed +++ sheet.nav +++ nav_pills +++ nav_inverse +++ nav_staticTop,
        fileItem,
        modelWizardItem,
        execItem,
        authenticationItem,
        marketItem,
        pluginItem,
        docItem
      )
    )
    maindiv.appendChild(tags.div(shutDown.shutdownButton))
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(modelWizardTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(authenticationTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(marketTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(pluginTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(environmentStackTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(docTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(AlertPanel.alertDiv)

    Settings.settings.foreach { sets ⇒
      maindiv.appendChild(
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
      ).render
      )
    }

    body.appendChild(maindiv)
    Plugins.load
  }

}