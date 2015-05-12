package org.openmole.gui.client.core

import org.openmole.core.workspace.Workspace
import org.openmole.gui.client.core.dataui.EditorPanelUI
import org.openmole.gui.client.core.files.{ FileNode, DirNode, TreeNodePanel }
import org.openmole.gui.client.core.files.TreeNodePanel._
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.DragEvent
import scalatags.JsDom.{ tags ⇒ tags }
import org.openmole.gui.misc.js.Forms._
import scala.scalajs.js.annotation.JSExport
import scala.util.{ Success, Failure }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.misc.js.JsRxTags._
import org.scalajs.{ jquery, dom }
import rx._
import scalatags.JsDom.all._
import org.openmole.gui.misc.js.Tabs
import org.openmole.gui.misc.js.Tabs._

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

    dom.document.body.appendChild(
      nav("mainNav",
        Seq(
          (navItem("executions", "Executions").render, "env", () ⇒ {
            println("Not yet")
          }),
          (navItem("files", "Files").render, "files", () ⇒ {
            openFileTree() = !openFileTree()
          })
        ), nav_pills + nav_inverse + nav_staticTop
      )
    )

    val maindiv = dom.document.body.appendChild(tags.div.render)

    Post[Api].workspacePath.call().foreach { projectsPath ⇒
      val treeNodePanel = TreeNodePanel(projectsPath)
      maindiv.appendChild(
        tags.div(`class` := Rx {
          if (openFileTree()) "show-nav"
          else ""
        }
        )(tags.div(id := "site-canvas")(
          tags.div(id := "site-menu")(
            treeNodePanel.view.render
          ),
          treeNodePanel.fileDisplayer.tabs.render
        ).render)
      )

      body.appendChild(maindiv)
    }

  }

}