package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ TreeNodeTabs, TreeNodePanel }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.Event
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.BootstrapTags._
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
    val body = dom.document.body
    val openFileTree = Var(false)

    implicit val executionTrigerrer = new PanelTriggerer {
      val modalPanel = new ExecutionPanel
    }

    val execItem = dialogNavItem("executions", "Executions", () ⇒ executionTrigerrer.trigger)

    val fileItem = navItem("files", "Files", todo = () ⇒ {
      openFileTree() = !openFileTree()
    })

    dom.document.body.appendChild(
      nav("mainNav",
        nav_pills + nav_inverse + nav_staticTop,
        execItem,
        fileItem
      )
    )

    val maindiv = dom.document.body.appendChild(tags.div.render)
    maindiv.appendChild(executionTrigerrer.modalPanel.dialog.render)

    Post[Api].workspacePath.call().foreach { projectsPath ⇒
      val treeNodePanel = TreeNodePanel(projectsPath)
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

      body.appendChild(maindiv)
    }

  }

}