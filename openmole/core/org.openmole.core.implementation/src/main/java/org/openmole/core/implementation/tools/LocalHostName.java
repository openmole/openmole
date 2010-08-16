/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.core.implementation.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author reuillon
 */
public class LocalHostName {

    String localHostName;

    static LocalHostName instance = new LocalHostName();

    private LocalHostName() {}

    public synchronized  String getNameForLocalHost() {
        if(localHostName == null) {
            try {
                localHostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException ex) {
                Logger.getLogger(LocalHostName.class.getName()).log(Level.WARNING, "Was not able to get local host name.", ex);
                localHostName = UUID.randomUUID().toString();
            }
        }

        return localHostName;
    }

    public static LocalHostName getInstance() {
        return instance;
    }

}
