package org.openmole.gui.plugin.environment.ssh.ext

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

import org.openmole.gui.ext.data.EnvironmentData

case class SSHEnvironmentData(
    val name: String = "",
    val login: String = "",
    val host: String = "",
    val nbSlots: Int = 1,
    val port: Int = 22,
    val dir: String = "/tmp/",
    val openMOLEMemory: Option[Int] = None,
    val threads: Option[Int] = None
) extends EnvironmentData {
}