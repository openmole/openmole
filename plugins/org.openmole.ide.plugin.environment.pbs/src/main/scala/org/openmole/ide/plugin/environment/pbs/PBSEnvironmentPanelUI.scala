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
import scala.swing.Label
import scala.swing.TextField

class SSHEnvironmentPanelUI(pud: PBSEnvironmentDataUI) extends PluginPanel("fillx,wrap 2", "", "") with IEnvironmentPanelUI {

  val loginTextField = new TextField(pud.login, 15)
  val hostTextField = new TextField(pud.host, 15)
  val dirTextField = new TextField(pud.dir, 15)
  val runTimeMemoryTextField = new TextField(pud.runtimeMemory.toString, 5)

  contents += (new Label("Login"), "gap para")
  contents += loginTextField

  contents += (new Label("Host"), "gap para")
  contents += hostTextField

  contents += (new Label("Directory"), "gap para")
  contents += dirTextField

  contents += (new Label("Runtime memory"), "gap para")
  contents += runTimeMemoryTextField

  override def saveContent(name: String) = new PBSEnvironmentDataUI(name,
    loginTextField.text,
    hostTextField.text,
    nbSlotTextField.text.toInt,
    dirTextField.text,
    runTimeMemoryTextField.text.toInt)
}
