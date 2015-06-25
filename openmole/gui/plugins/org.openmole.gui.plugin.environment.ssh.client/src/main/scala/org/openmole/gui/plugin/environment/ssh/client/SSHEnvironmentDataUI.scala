package org.openmole.gui.plugin.environment.ssh.client

/*
 * Copyright (C) 16/06/2015 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.plugin.environment.ssh.ext.SSHEnvironmentData
import org.openmole.gui.client.core.dataui.EnvironmentDataUI
import org.openmole.gui.client.core.ClientService
import ClientService._
import scala.scalajs.js.annotation.JSExport
import rx._
import ClientService._

class SSHEnvironmentDataUI(val name: Var[String] = Var(""),
                           val login: Var[String] = Var(""),
                           val host: Var[String] = Var(""),
                           val nbSlots: Var[Int] = Var(1),
                           val port: Var[Int] = Var(22),
                           val dir: Var[String] = Var("/tmp/"),
                           val openMOLEMemory: Var[Option[Int]] = Var(None),
                           val nbThread: Var[Option[Int]] = Var(None)) extends EnvironmentDataUI {

  def data = new SSHEnvironmentData(name, login, host, nbSlots, port, dir, openMOLEMemory, nbThread)

  def panelUI = new SSHEnvironmentPanelUI(this)

  def dataType = "SSH"
}