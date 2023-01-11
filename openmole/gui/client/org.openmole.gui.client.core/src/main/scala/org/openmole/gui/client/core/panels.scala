package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{AlertPanel, BannerAlert}
import org.openmole.gui.client.core.files.{FileDisplayer, TreeNodeManager, TreeNodePanel, TreeNodeTabs}
import org.openmole.gui.ext.data.{ErrorManager, GUIPluginAsJS, PluginServices, WizardPluginFactory}
import com.raquo.laminar.api.L._

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

  val pluginServices =
    PluginServices(
      errorManager = new ErrorManager {
        override def signal(message: String, stack: Option[String]): Unit = panels.bannerAlert.registerWithStack(message, stack)
      }
    )

  lazy val treeNodeManager = new TreeNodeManager()

  lazy val treeNodeTabs = new TreeNodeTabs()

  lazy val fileDisplayer =
    new FileDisplayer(
      treeNodeTabs = treeNodeTabs
    )

  def openExecutionPanel = {
    ExecutionPanel.setTimerOn
    ExecutionPanel.updateStaticInfos
    ExecutionPanel.updateExecutionInfo
    panels.expandTo(ExecutionPanel.render,4)
  }
  
  lazy val treeNodePanel =
    new TreeNodePanel(
      treeNodeManager = treeNodeManager,
      fileDisplayer = fileDisplayer,
      showExecution = () ⇒ openExecutionPanel,
      treeNodeTabs = treeNodeTabs,
      services = pluginServices)

  def urlImportPanel =
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

  val pluginPanel = new PluginPanel(treeNodeManager)

  lazy val marketPanel = new MarketPanel(treeNodeManager)

  lazy val stackPanel = new TextPanel("Error stack")
  lazy val settingsView = new SettingsView(fileDisplayer)
  lazy val connection = new Connection

  lazy val bannerAlert =
    new BannerAlert(
      resizeTabs = () ⇒ treeNodeTabs.tabsElement.tabs.now().foreach { t ⇒ t.t.resizeEditor }
    )

  lazy val alertPanel = new AlertPanel()

}
