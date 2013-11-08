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

import org.openmole.ide.osgi.netlogo4.NetLogo4
import org.openmole.ide.core.implementation.data.TaskDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._

class NetLogo4TaskPanelUI(ndu: NetLogo4TaskDataUI010) extends GenericNetLogoPanelUI(ndu.nlogoPath,
  ndu.workspaceEmbedded,
  ndu.lauchingCommands,
  ndu.prototypeMappingInput,
  ndu.prototypeMappingOutput,
  ndu.resources) {
  override def saveContent(name: String): TaskDataUI = new NetLogo4TaskDataUI010(name,
    workspaceCheckBox.selected,
    nlogoTextField.text,
    launchingCommandTextArea.text,
    Converters.flattenTuple2Options(multiProtoString.content.map { c ⇒ (c.comboValue1, c.comboValue2) }).filter { case (p, s) ⇒ Proxies.check(p) },
    Converters.flattenTuple2Options(multiStringProto.content.map { c ⇒ (c.comboValue1, c.comboValue2) }).filter { case (s, p) ⇒ Proxies.check(p) },
    resourcesMultiTextField.content.map { _.content })

  def buildNetLogo = new NetLogo4
}
