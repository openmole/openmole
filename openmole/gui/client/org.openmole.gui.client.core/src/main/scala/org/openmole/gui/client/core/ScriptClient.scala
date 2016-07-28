package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{ AlertPanel, AbsolutePositioning }
import AbsolutePositioning.CenterPagePosition
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ KeyboardEvent, HTMLElement, HTMLFormElement }
import org.openmole.gui.client.core.panels._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.OMTags
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.dom
import rx._
import scalatags.JsDom.all._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import sheet._

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
  def run(): Unit = {

    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
    val alert: Var[Boolean] = Var(false)

    val shutdownButton = span(
      omsheet.resetBlock,
      a(onclick := { () ⇒
        alert() = true
      }, omsheet.resetPassword)("Reset password"),
      a(
        omsheet.shutdownButton,
        bs.glyphSpan(glyph_off, onclickAction = () ⇒
          AlertPanel.string(
            "This will stop the server, the application will no longer be usable. Halt anyway?",
            () ⇒ {
              treeNodePanel.fileDisplayer.tabs.saveAllTabs(() ⇒
                dom.window.location.href = "shutdown")
            },
            transform = CenterPagePosition
          ))
      )
    )

    val passwordChosen = Var(true)
    val passwordOK = Var(false)

    OMPost[Api].passwordState().call().foreach { b ⇒
      passwordChosen() = b.chosen
      passwordOK() = b.hasBeenSet
    }

    val body = dom.document.body
    val maindiv = body.appendChild(tags.div())

    val passwordInput = bs.input("")(
      placeholder := "Password",
      `type` := "password",
      width := "130px",
      autofocus := true
    ).render

    val passwordAgainInput = bs.input("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      autofocus
    ).render

    def cleanInputs = {
      passwordInput.value = ""
      passwordAgainInput.value = ""
    }

    def resetPassword = OMPost[Api].resetPassword().call().foreach { b ⇒
      passwordChosen() = false
      passwordOK() = false
      cleanInputs
    }

    val authenticationPanel = new AuthenticationPanel

    def setPassword(s: String) = OMPost[Api].setPassword(s).call().foreach { b ⇒
      passwordOK() = b
      cleanInputs
    }

    lazy val connectButton = bs.button("Connect", btn_primary, () ⇒ connection).render

    def connection: Unit = {
      if (passwordChosen.now) setPassword(passwordInput.value)
      else if (passwordInput.value == passwordAgainInput.value) {
        passwordChosen() = true
        setPassword(passwordInput.value)
      }
      else cleanInputs
    }

    def connectionForm(i: HTMLElement): HTMLFormElement =
      tags.form(i, `type` := "submit", onsubmit := { () ⇒
        connection
        false
      }).render

    val connectionDiv = Rx {
      div(
        if (!passwordOK()) omsheet.connectionTabOverlay
        else omsheet.displayOff
      )(div(
          zIndex := 1101,
          img(src := "img/openmole.png", omsheet.openmoleLogo),
          // openmoleText,
          shutdownButton,
          div(
            if (!passwordOK()) omsheet.centerPage else emptyMod,
            div(
              if (alert())
                AlertPanel.string(
                "Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
                () ⇒ {
                  alert() = false
                  resetPassword
                }, () ⇒ {
                  alert() = false
                }, CenterPagePosition
              )
              else {
                div(
                  omsheet.connectionBlock,
                  connectionForm(passwordInput),
                  if (!passwordChosen()) connectionForm(passwordAgainInput) else div(),
                  connectButton
                )
              }
            )
          )
        ))
    }

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
    maindiv.appendChild(tags.div(shutdownButton))
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(modelWizardTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(authenticationTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(marketTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(pluginTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(environmentStackTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(docTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(AlertPanel.alertDiv)

    Settings.workspacePath.foreach { projectsPath ⇒
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
            tags.img(src := "img/version.svg", `class` := "logoVersion"),
            tags.div("Loving Lobster", `class` := "textVersion")
          )

      ).render
      )
    }

    body.appendChild(connectionDiv)
    body.appendChild(maindiv)
  }

}