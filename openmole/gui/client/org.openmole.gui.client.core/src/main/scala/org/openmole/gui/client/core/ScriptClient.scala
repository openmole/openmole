package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ TreeNodeTabs, TreeNodePanel }
import org.openmole.gui.misc.js.BootstrapTags
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.Event
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

    def isPasswordChosen(todoIfChosen: () ⇒ Unit = () ⇒ {}) = OMPost[Api].passwordChosen().call().foreach { b ⇒
      passwordChosen() = b
      if (b) todoIfChosen()
    }

    isPasswordChosen()

    val body = dom.document.body
    val maindiv = body.appendChild(tags.div())

    val passwordInput = bs.input("")(
      placeholder := "Password",
      `type` := "password",
      width := "130px",
      autofocus
    ).render

    val passwordAgainInput = bs.input("")(
      placeholder := "Password again",
      `type` := "password",
      width := "130px",
      autofocus
    ).render

    def setPassword(s: String) = OMPost[Api].setPassword(s).call().foreach { b ⇒
      passwordOK() = b
      passwordInput.value = ""
    }

    lazy val connectButton = bs.button("Connect")(onclick := { () ⇒
      connection
    }).render

    def connection: Unit = {
      if (passwordChosen()) setPassword(passwordInput.value)
      else if (passwordInput.value == passwordAgainInput.value) {
        passwordChosen() = true
        connection
      }
    }

    def resetPassword = OMPost[Api].resetPassword().call().foreach { b ⇒
      passwordChosen() = false
      passwordOK() = false
      passwordInput.value = ""
      passwordAgainInput.value = ""
    }

    val connectionForm = tags.div(`class` := Rx {
      if (!passwordOK()) "connectionTabOverlay" else "displayOff"
    })(
      tags.div(`class` := Rx {
        if (!passwordOK()) "centerPage" else ""
      }, tags.form(`type` := "submit", onsubmit := { () ⇒
        connection
        false
      }),
        Rx {
          bs.inputGroup(emptyCK) /*(navbar_left)*/ (
            passwordInput,
            if (!passwordChosen()) passwordAgainInput else tags.div(),
            inputGroupButton(connectButton)
          )
        },
        tags.div(
          tags.a(onclick := { () ⇒ resetPassword
          }, cursor := "pointer")("Reset password")
        )
      )
    )

    val openFileTree = Var(false)

    implicit val executionTriggerer = new PanelTriggerer {
      val modalPanel = new ExecutionPanel
    }

    val authenticationTriggerer = new PanelTriggerer {
      val modalPanel = new AuthenticationPanel
    }

    val execItem = dialogNavItem("executions", "Executions", () ⇒ executionTriggerer.trigger)

    val authenticationItem = dialogNavItem("authentications", "Authentications", () ⇒ authenticationTriggerer.trigger)

    val fileItem = navItem("files", "Files", todo = () ⇒ {
      openFileTree() = !openFileTree()
    })

    val passItem = navItem("pass", "Pass", todo = () ⇒ {
      resetPassword
    })

    maindiv.appendChild(
      nav("mainNav",
        nav_pills + nav_inverse + nav_staticTop,
        fileItem,
        execItem,
        authenticationItem,
        passItem
      )
    )
    maindiv.appendChild(executionTriggerer.modalPanel.dialog.render)

    OMPost[Api].workspacePath.call().foreach { projectsPath ⇒
      val treeNodePanel = TreeNodePanel(projectsPath)(executionTriggerer)
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
    }

    body.appendChild(connectionForm)
    body.appendChild(maindiv)
  }

}