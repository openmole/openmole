package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{AlertPanel, BannerAlert}
import org.openmole.gui.client.core.files.{FileDisplayer, TabContent, TreeNodeManager, TreeNodePanel, TreeNodeTabs}
import org.openmole.gui.shared.data.{GUIPluginAsJS}
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.data.{ErrorManager, PluginServices, WizardPluginFactory}

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

  def closeExpandable(using panels: Panels) = panels.expandablePanel.set(None)

  def expandTo(el: HtmlElement, id: Int)(using panels: Panels) = panels.expandablePanel.update {
    _ match {
      case Some(ep: ExpandablePanel) ⇒ if (ep.id == id) None else Some(ExpandablePanel(id, el))
      case None ⇒ Some(ExpandablePanel(id, el))
    }
  }

  def urlImportPanel(treeNodeManager: TreeNodeManager, bannerAlert: BannerAlert) =
    new URLImportPanel(
      treeNodeManager,
      bannerAlert = bannerAlert)

  def apply() =
    val expandablePanel: Var[Option[Panels.ExpandablePanel]] = Var(None)
    val treeNodeTabs = new TreeNodeTabs
    val alertPanel = new AlertPanel
    val bannerAlert = new BannerAlert
    val tabContent = new TabContent
    val pluginPanel = new PluginPanel
    val fileDisplayer = new FileDisplayer
    val executionPanel = new ExecutionPanel
    val treeNodePanel = new TreeNodePanel
    val settingsView = new SettingsView
    val connection = new Connection

    new Panels(treeNodePanel, tabContent, pluginPanel, fileDisplayer, settingsView, executionPanel, bannerAlert, treeNodeTabs, alertPanel, connection, expandablePanel)

case class Panels(
  treeNodePanel: TreeNodePanel,
  tabContent: TabContent,
  pluginPanel: PluginPanel,
  fileDisplayer: FileDisplayer,
  settingsView: SettingsView,
  executionPanel: ExecutionPanel,
  bannerAlert: BannerAlert,
  treeNodeTabs: TreeNodeTabs,
  alertPanel: AlertPanel,
  connection: Connection,
  expandablePanel: Var[Option[Panels.ExpandablePanel]])

