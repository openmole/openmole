/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.plugin.task.netlogo

import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.osgi.netlogo5.NetLogo5
import scala.swing._
import swing.Swing._

class NetLogo5TaskPanelUI(ndu: NetLogo5TaskDataUI) extends GenericNetLogoPanelUI(ndu.nlogoPath,
  ndu.workspaceEmbedded,
  ndu.lauchingCommands,
  ndu.prototypeMappingInput,
  ndu.prototypeMappingOutput,
  ndu.resources) {
  override def saveContent(name: String): ITaskDataUI = new NetLogo5TaskDataUI(name,
    workspaceCheckBox.selected,
    nlogoTextField.text,
    launchingCommandTextArea.text,
    if (multiProtoString.isDefined)
      multiProtoString.get.content.map { c ⇒ (c.comboValue1.get, c.comboValue2.get) }.filterNot(_._1.dataUI.isInstanceOf[EmptyPrototypeDataUI])
    else List(),
    if (multiStringProto.isDefined)
      multiStringProto.get.content.map { c ⇒ (c.comboValue1.get, c.comboValue2.get) }.filterNot(_._2.dataUI.isInstanceOf[EmptyPrototypeDataUI])
    else List(),
    resourcesMultiTextField.content.map { _.content })

  def buildNetLogo = new NetLogo5

}
