/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.plugin.task.netlogo4

import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.plugin.task.netlogo.GenericNetLogoPanelUI
import scala.swing._
import swing.Swing._

class NetLogo4TaskPanelUI(ndu: NetLogo4TaskDataUI) extends GenericNetLogoPanelUI(ndu.nlogoPath,
                                                                              ndu.workspacePath,
                                                                              ndu.lauchingCommands) with ITaskPanelUI{
  override def saveContent(name: String): ITaskDataUI = new NetLogo4TaskDataUI(name, workspaceTextField.text, nlogoTextField.text, launchingCommandTextArea.text)
}
