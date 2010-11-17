/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.tools

import java.net.InetAddress
import java.util.UUID
import java.util.logging.Logger
import java.util.logging.Level

object LocalHostName {

  @transient
  lazy val localHostName = 
    try {
      InetAddress.getLocalHost.getCanonicalHostName
    } catch {
      case ex =>
        Logger.getLogger(LocalHostName.getClass.getName).log(Level.WARNING, "Was not able to get local host name.", ex)
        UUID.randomUUID.toString
    }

}
