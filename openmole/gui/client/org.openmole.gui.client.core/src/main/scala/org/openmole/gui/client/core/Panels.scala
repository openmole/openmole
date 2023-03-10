package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{FileDisplayer, TabContent, TreeNodeManager, TreeNodePanel, TreeNodeTabs}
import org.openmole.gui.shared.data.GUIPluginAsJS
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext.{ErrorManager, NotificationService, NotificationLevel, PluginServices, ServerAPI, WizardPluginFactory}

/*
 * Copyright (C) 24/07/15 // mathieu.leclaire@openmole.org
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

object Panels:
  case class ExpandablePanel(id: Int, element: HtmlElement)

  object ExpandablePanel:
    def toString(id: Int) =
      id match
        case 5 => "SETTINGS"
        case 4 => "EXECUTIONS"
        case 2 => "AUTHENTICATIONS"
        case 1 => "PLUGINS"
        case _ => ""

  def closeExpandable(using panels: Panels) = panels.expandablePanel.set(None)

  def expandTo(el: HtmlElement, id: Int)(using panels: Panels) = panels.expandablePanel.update {
    _ match {
      case Some(ep: ExpandablePanel) ⇒ if (ep.id == id) None else Some(ExpandablePanel(id, el))
      case None ⇒ Some(ExpandablePanel(id, el))
    }
  }

  def apply() =
    val expandablePanel: Var[Option[Panels.ExpandablePanel]] = Var(None)
    val treeNodeTabs = new TreeNodeTabs
    val tabContent = new TabContent
    val pluginPanel = new PluginPanel
    val fileDisplayer = new FileDisplayer
    val executionPanel = new ExecutionPanel
    val treeNodePanel = new TreeNodePanel
    val connection = new Connection
    val notification = new NotificationManager
    
    new Panels(treeNodePanel, tabContent, pluginPanel, fileDisplayer, executionPanel, treeNodeTabs, notification, connection, expandablePanel)


//  def initialize(using fetch: Fetch, api: ServerAPI, panels: Panels) =
//    TreeNodeTabs.setObservers


case class Panels(
  treeNodePanel: TreeNodePanel,
  tabContent: TabContent,
  pluginPanel: PluginPanel,
  fileDisplayer: FileDisplayer,
  executionPanel: ExecutionPanel,
  treeNodeTabs: TreeNodeTabs,
  notifications: NotificationManager,
  connection: Connection,
  expandablePanel: Var[Option[Panels.ExpandablePanel]])


