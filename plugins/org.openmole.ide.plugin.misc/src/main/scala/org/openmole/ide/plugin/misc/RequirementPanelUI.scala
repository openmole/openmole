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

package org.openmole.ide.plugin.environment.glite

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.misc.widget._
import org.openmole.misc.workspace.Workspace
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class GliteEnvironmentPanelUI(val architecture64: Boolean = false,
                              val runtimeMemory: String = Workspace.preference(BatchEnvironment.MemorySizeForRuntime),
                              val workerNodeMemory: String = "",
                              val maxCPUTime: String = "",
                              val otherRequirements: String = "") extends TabbedPane.Page("Requirements", new Label) {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val architectureCheckBox = new CheckBox("64 bits") { tooltip = Help.tooltip(i18n.getString("64bits")) }

  val runtimeMemoryLabel = new Label("RuntimeMemory")
  val runtimeMemoryTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("runtimeMemory"),
      i18n.getString("runtimeMemoryEx"))
  }
  val workerNodeMemoryLabel = new Label("Worker memory")
  val workerNodeMemoryTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("workerNodeMemory"),
      i18n.getString("workerNodeMemoryEx"))
  }
  val maxCPUTimeLabel = new Label("Max CPU Time")
  val maxCPUTimeTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("maxCPUTime"),
      i18n.getString("maxCPUTimeEx"))
  }
  val otherRequirementLabel = new Label("Other")
  val otherRequirementTextField = new TextField(16) { tooltip = Help.tooltip(i18n.getString("other")) }

  this.content = new PluginPanel("wrap 2") {
    contents += (architectureCheckBox, "wrap")
    contents += (runtimeMemoryLabel, "gap para")
    contents += runtimeMemoryTextField
    contents += (workerNodeMemoryLabel, "gap para")
    contents += workerNodeMemoryTextField
    contents += (maxCPUTimeLabel, "gap para")
    contents += maxCPUTimeTextField
    contents += otherRequirementLabel
    contents += otherRequirementTextField
  }

  architectureCheckBox.selected = architecture64
  runtimeMemoryTextField.text = runtimeMemory
  workerNodeMemoryTextField.text = workerNodeMemory
  maxCPUTimeTextField.text = maxCPUTime
  otherRequirementTextField.text = otherRequirements
}
