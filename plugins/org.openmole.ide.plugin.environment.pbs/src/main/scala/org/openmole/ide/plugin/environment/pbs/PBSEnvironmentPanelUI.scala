/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.environment.pbs

import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.plugin.environment.tools._
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class PBSEnvironmentPanelUI(pud: PBSEnvironmentDataUI) extends PluginPanel("fillx,wrap 2", "", "") with IEnvironmentPanelUI {

  val loginTextField = new TextField(pud.login, 15)
  val hostTextField = new TextField(pud.host, 15)
  val dirTextField = new TextField(pud.dir, 15)
  val queueTextField = new TextField(pud.queue, 15)
  val runTimeMemoryTextField = new TextField(pud.runtimeMemory.toString, 5)

  val requirementsPanelUI = new RequirementPanelUI(pud.requirements)

  val tabbedPane = new TabbedPane
  tabbedPane.pages += new TabbedPane.Page("PBS settings",
    new PluginPanel("wrap 2") {
      contents += (new Label("Login"), "gap para")
      contents += loginTextField

      contents += (new Label("Host"), "gap para")
      contents += hostTextField

      contents += (new Label("Directory"), "gap para")
      contents += dirTextField

      contents += (new Label("Runtime memory"), "gap para")
      contents += runTimeMemoryTextField
    })

  tabbedPane.pages += requirementsPanelUI
  contents += tabbedPane

  override def saveContent(name: String) =
    new PBSEnvironmentDataUI(name,
      loginTextField.text,
      hostTextField.text,
      dirTextField.text,
      queueTextField.text,
      runTimeMemoryTextField.text.toInt,
      new RequirementDataUI(
        requirementsPanelUI.architectureCheckBox.selected,
        requirementsPanelUI.workerNodeMemoryTextField.text,
        requirementsPanelUI.maxCPUTimeTextField.text,
        requirementsPanelUI.otherRequirementTextField.text))
}
