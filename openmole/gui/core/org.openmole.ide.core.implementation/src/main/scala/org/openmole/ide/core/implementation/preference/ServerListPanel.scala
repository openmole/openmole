/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core.implementation.preference

import org.openmole.ide.misc.widget.{ LinkLabel, PluginPanel }
import org.openmole.ide.misc.widget.multirow.{ MultiPanel, IFactory, IData, IPanel }
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.core.implementation.dialog.DialogFactory
import scala.swing.Action
import org.openmole.misc.workspace.Workspace

object ServerListPanel {
  def list = try {
    Workspace.persistent("gui").load("servers") match {
      case l: List[String] ⇒ l
      case _               ⇒ List()
    }
  }
  catch {
    case _: Throwable ⇒ List()
  }
}

import ServerListPanel._
class ServerListPanel extends PluginPanel("wrap", "[grow,fill]", "") {

  class ServerPanel(var data: ServerData)
      extends PluginPanel("") with IPanel[ServerData] {

    val linkLabel = new LinkLabel(
      expandName,
      new Action("") {
        def apply = displayPopup
      },
      4,
      "73a5d2",
      false)

    contents += linkLabel

    def displayPopup: Unit = {
      data = new ServerData(DialogFactory.serverURL(data.serverUrl))
      linkLabel.link(expandName)

      revalidate
      repaint
    }

    def expandName = {
      if (data.serverUrl == "") "new"
      else data.serverUrl
    }

    def content = data

  }

  class ServerData(val serverUrl: String = "") extends IData

  class ServerFactory extends IFactory[ServerData] {
    def apply = new ServerPanel(new ServerData)
  }

  lazy val multiPanel = new MultiPanel("Server list",
    new ServerFactory,
    list.map { s ⇒ new ServerPanel(new ServerData(s)) },
    CLOSE_IF_EMPTY,
    ADD)

  contents += multiPanel.panel

  def save = Workspace.persistent("gui").save(multiPanel.content.map { _.serverUrl }, "servers")
}