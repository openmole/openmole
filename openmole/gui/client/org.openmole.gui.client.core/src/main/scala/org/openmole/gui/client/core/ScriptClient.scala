package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ TreeNode, DirNode, TreeNodePanel }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.{ HTMLElement, HTMLFormElement }
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs }
import bs._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.dom
import rx._
import scalatags.JsDom.all._

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

    val connectionDiv = tags.div(`class` := Rx {
      if (!passwordOK()) "connectionTabOverlay" else "displayOff"
    })(
      tags.div(`class` := Rx {
        if (!passwordOK()) "centerPage" else ""
      },
        Rx {
          tags.div(
            connectionForm(tags.span(passwordInput,
              tags.a(onclick := { () ⇒ resetPassword
              }, cursor := "pointer")("Reset password")
            ).render),
            if (!passwordChosen()) connectionForm(passwordAgainInput) else tags.div(),
            connectButton
          )
        }
      )
    )

    val openFileTree = Var(true)

    implicit val executionTriggerer = new PanelTriggerer {
      val modalPanel = new ExecutionPanel
    }

    val authenticationTriggerer = new PanelTriggerer {
      val modalPanel = authenticationPanel
    }

    val execItem = dialogGlyphNavItem("executions", glyph_settings, () ⇒ executionTriggerer.triggerOpen)

    val authenticationItem = dialogGlyphNavItem("authentications", glyph_lock, () ⇒ authenticationTriggerer.triggerOpen)

    val fileItem = dialogGlyphNavItem("files", glyph_file, todo = () ⇒ {
      openFileTree() = !openFileTree()
    })

    maindiv.appendChild(
      nav("mainNav",
        nav_pills + nav_inverse + nav_staticTop,
        fileItem,
        execItem,
        authenticationItem
      )
    )
    maindiv.appendChild(tags.div(`class` := "openMOLETitle", "OpenMOLE - 5.0").render)
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)
    maindiv.appendChild(authenticationTriggerer.modalPanel.dialog.render)

    Settings.workspaceProjectNode.foreach { projectsPath ⇒
      TreeNode.treeNodeDataToTreeNode(projectsPath) match {
        case dn: DirNode ⇒
          val treeNodePanel = TreeNodePanel(dn)(executionTriggerer)
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
              })(treeNodePanel.fileDisplayer.tabs.render)

            ).render
          )
        case _ ⇒
      }
    }

    body.appendChild(connectionDiv)
    body.appendChild(maindiv)
  }

}