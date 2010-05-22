/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.misc.tools.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Network {

    public static boolean IsLocalHost(String hostName) {
        InetAddress host;
        try {
            host = InetAddress.getByName(hostName);

            //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,"IP of host" + host.getHostAddress());

            if (host.isLoopbackAddress()) {
                return true;
            }

            InetAddress localhost = InetAddress.getLocalHost();

            // Just in case this host has multiple IP addresses....
            InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());

            for (InetAddress add : allMyIps) {
                //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,"IP of localhost" + add.getHostAddress());
                if (add.equals(host)) {
                    return true;
                }
            }

            return false;
        } catch (UnknownHostException e) {
            Logger.getLogger(Network.class.getName()).log(Level.WARNING, "Host not found " + hostName, e);
            return false;
        }
    }

    public static interface IConnectable {
        void connect(int port) throws Exception;
    }

    public static synchronized int ConnectToFreePort(IConnectable connectable) throws Exception {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();

        connectable.connect(port);

        return port;
    }
}
