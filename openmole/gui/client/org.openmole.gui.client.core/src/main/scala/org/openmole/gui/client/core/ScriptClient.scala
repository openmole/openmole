package org.openmole.gui.client.core

import org.openmole.gui.client.core.AbsolutePositioning.{ RightTransform, TopZone, CenterTransform }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ HTMLElement, HTMLFormElement }
import org.openmole.gui.client.core.panels._
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, ToolTip }
import bs._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.dom
import rx._
import scalatags.JsDom.all._
import org.scalajs.jquery

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

    val shutdownButton =
      a(`class` := "shutdownButton",
        bs.glyph(glyph_off),
        cursor := "pointer",
        onclick := { () ⇒
          AlertPanel.popup("This will stop the server, the application will no longer be usable. Halt anyway?",
            () ⇒ {
              treeNodePanel.fileDisplayer.tabs.saveAllTabs(() ⇒
                dom.window.location.href = "shutdown"
              )
            },
            transform = RightTransform(),
            zone = TopZone())
        })

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

    val authenticationPanel = new AuthenticationPanel(() ⇒ {
      resetPassword
    }
    )

    def setPassword(s: String) = OMPost[Api].setPassword(s).call().foreach { b ⇒
      passwordOK() = b
      cleanInputs
    }

    lazy val connectButton = bs.button("Connect", btn_primary)(onclick := { () ⇒
      connection
    }).render

    def connection: Unit = {
      if (passwordChosen()) setPassword(passwordInput.value)
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
      }
      ).render

    val alert: Var[Boolean] = Var(false)

    val openmoleText = tags.div(
      tags.h1(`class` := "openmole-connection openmole-pen openmole-pen-connection-position")("pen"),
      tags.h1(`class` := "openmole-connection openmole-mole openmole-mole-connection-position")("MOLE")
    )

    val connectionDiv = tags.div(`class` := Rx {
      if (!passwordOK()) "connectionTabOverlay" else "displayOff"
    })(
      tags.div(
        tags.img(src := "img/openmole.png", `class` := "openmole-logo"),
        openmoleText,
        shutdownButton,
        tags.div(`class` := Rx {
          if (!passwordOK()) "centerPage" else ""
        },
          Rx {
            tags.div(
              if (alert())
                AlertPanel.popup("Careful! Resetting your password will wipe out all your preferences! Reset anyway?",
                () ⇒ {
                  alert() = false
                  resetPassword
                }, () ⇒ {
                  alert() = false
                }, CenterTransform())
              else {
                tags.div(
                  connectionForm(
                    tags.span(passwordInput,
                      tags.a(onclick := { () ⇒
                        alert() = true
                      }, cursor := "pointer")("Reset password")).render),
                  if (!passwordChosen()) connectionForm(passwordAgainInput) else tags.div(),
                  connectButton
                )
              }
            )
          }
        )
      )
    )

    val openFileTree = Var(true)

    val authenticationTriggerer = new PanelTriggerer {
      val modalPanel = authenticationPanel
    }

    val execItem = dialogGlyphNavItem("executions", glyph_settings, () ⇒ executionTriggerer.triggerOpen, help = ToolTip("Executions"))

    val authenticationItem = dialogGlyphNavItem("authentications", glyph_lock, () ⇒ authenticationTriggerer.triggerOpen, help = ToolTip("Authentications"))

    val marketItem = dialogGlyphNavItem("market", glyph_market, () ⇒ marketTriggerer.triggerOpen, help = ToolTip("Market place"))

    val pluginItem = dialogGlyphNavItem("plugin", glyph_plug, () ⇒ pluginTriggerer.triggerOpen, help = ToolTip("Plugins"))

    val envItem = dialogGlyphNavItem("envError", glyph_exclamation, () ⇒ environmentStackTriggerer.open)

    val docItem = dialogGlyphNavItem("doc", glyph_comment, () ⇒ docTriggerer.open, help = ToolTip("Documentation"))

    val fileItem = dialogGlyphNavItem("files", glyph_file, todo = () ⇒ {
      openFileTree() = !openFileTree()
    }, help = ToolTip("Files"))

    maindiv.appendChild(
      nav("mainNav",
        nav_pills + nav_inverse + nav_staticTop,
        fileItem,
        execItem,
        authenticationItem,
        marketItem,
        pluginItem,
        docItem
      )
    )
    maindiv.appendChild(tags.div(
      tags.h1(`class` := "openmole-pen openmole-small openmole-pen-small-position")("Open"),
      tags.h1(`class` := "openmole-mole openmole-small openmole-mole-small-position")("MOLE"),
      tags.h1(`class` := "openmole-small openmole-version")("5"),
      shutdownButton
    ))
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(authenticationTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(marketTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(pluginTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(environmentStackTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(docTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(AlertPanel.div)

    Settings.workspacePath.foreach { projectsPath ⇒
      maindiv.appendChild(
        tags.div(`class` := "fullpanel")(
          tags.div(`class` := Rx {
            "leftpanel " + {
              if (openFileTree()) "open" else ""
            }
          })(treeNodePanel.view.render),
          tags.div(`class` := Rx {
            "centerpanel " + {
              if (openFileTree()) "reduce" else ""
            }
          })(treeNodePanel.fileDisplayer.tabs.render,
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