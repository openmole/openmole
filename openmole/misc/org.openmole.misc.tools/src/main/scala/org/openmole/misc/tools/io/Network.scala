/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.io

import java.net.InetAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import java.util.logging.Level
import java.util.logging.Logger

object Network {
  def isLocalHost(hostName: String): Boolean = {
    
    try {
      val host = InetAddress.getByName(hostName)
      if (host.isLoopbackAddress) return true
            
      val localhost = InetAddress.getLocalHost

      // Just in case this host has multiple IP addresses....
      for (add <- InetAddress.getAllByName(localhost.getCanonicalHostName)) {
        if (add.equals(host)) return true
      }

      return false
    } catch {
      case e: UnknownHostException =>
        Logger.getLogger(Network.getClass.getName).log(Level.WARNING, "Host not found " + hostName, e);
        return false
    }
  }

  trait IConnectable {
    @throws(classOf[Throwable])
    def connect(port: Int)
  }

  def connectToFreePort(connectable: IConnectable): Int = synchronized {
    val server = new ServerSocket(0)
    val port = server.getLocalPort
    server.close
    connectable.connect(port)
    port
  }
}
