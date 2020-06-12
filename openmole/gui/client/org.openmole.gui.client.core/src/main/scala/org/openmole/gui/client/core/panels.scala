package org.openmole.gui.client.core

import org.openmole.gui.client.core.files.{ FileDisplayer, TreeNodeManager, TreeNodePanel, TreeNodeTabs }
import org.openmole.gui.ext.data.{ GUIPluginAsJS, WizardPluginFactory }

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

object panels {
  lazy val treeNodeManager = new TreeNodeManager()
  lazy val executionPanel = new ExecutionPanel
  lazy val treeNodeTabs = new TreeNodeTabs()

  lazy val fileDisplayer = new FileDisplayer(treeNodeTabs)
  lazy val treeNodePanel = new TreeNodePanel(treeNodeManager, fileDisplayer)

  def modelWizardPanel(wizards: Seq[WizardPluginFactory]) = new ModelWizardPanel(treeNodeManager, wizards)
  def urlImportPanel = new URLImportPanel(treeNodeManager)

  lazy val marketPanel = new MarketPanel(treeNodeManager)
  lazy val pluginPanel = new PluginPanel
  lazy val stackPanel = new TextPanel("Error stack")
  lazy val settingsView = new SettingsView(fileDisplayer)
  lazy val connection = new Connection
}
