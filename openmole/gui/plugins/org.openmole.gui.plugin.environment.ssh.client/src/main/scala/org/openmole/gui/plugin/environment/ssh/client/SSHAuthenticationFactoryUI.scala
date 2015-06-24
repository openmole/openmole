package org.openmole.gui.plugin.environment.ssh.client

import scala.scalajs.js.annotation.JSExport
import org.openmole.gui.ext.dataui.{AuthenticationFactoryUI, PanelUI, FactoryWithPanelUI}


/*
 * Copyright (C) 16/06/15 // mathieu.leclaire@openmole.org
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

@JSExport("org.openmole.gui.plugin.environment.ssh.client.SSHAuthenticationFactoryUI")
class SSHAuthenticationFactoryUI extends AuthenticationFactoryUI{

  val name = "SSH"

  def panelUI: PanelUI = new SSHAuthenticationPanelUI

}
