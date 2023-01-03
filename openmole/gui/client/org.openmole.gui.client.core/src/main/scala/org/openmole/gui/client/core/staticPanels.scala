package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{AlertPanel, BannerAlert}
import org.openmole.gui.client.core.files.{FileDisplayer, TabContent, TreeNodeManager, TreeNodePanel, TreeNodeTabs}
import org.openmole.gui.ext.data.{ErrorManager, GUIPluginAsJS, PluginServices, WizardPluginFactory}
import com.raquo.laminar.api.L.*

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

case class Panels(
  treeNodePanel: TreeNodePanel,
  tabContent: TabContent,
  treeNodeManager: TreeNodeManager,
  pluginPanel: PluginPanel,
  fileDisplayer: FileDisplayer,
  settingsView: SettingsView,
  pluginServices: PluginServices,
  executionPanel: ExecutionPanel)

object staticPanels {
  
  lazy val treeNodeTabs = new TreeNodeTabs()
  

  def urlImportPanel(treeNodeManager: TreeNodeManager) =
    new URLImportPanel(
      treeNodeManager,
      bannerAlert = bannerAlert)

  case class ExpandablePanel(id: Int, element: HtmlElement)

  val expandablePanel: Var[Option[ExpandablePanel]] = Var(None)
  //val openExpandablePanel = Var(false)

  def closeExpandable = expandablePanel.set(None)

  def expandTo(el: HtmlElement, id: Int) = expandablePanel.update {
    _ match {
      case Some(ep: ExpandablePanel) ⇒ if(ep.id == id) None else Some(ExpandablePanel(id, el))
      case None ⇒ Some(ExpandablePanel(id,el))
    }
  }

  //val pluginPanel = new PluginPanel

  //lazy val marketPanel = new MarketPanel(treeNodeManager)

  lazy val stackPanel = new TextPanel("Error stack")
  lazy val connection = new Connection

  lazy val bannerAlert =
    new BannerAlert(
      resizeTabs = () ⇒ treeNodeTabs.tabsElement.tabs.now().foreach { t ⇒ t.t.resizeEditor }
    )

  lazy val alertPanel = new AlertPanel

}
