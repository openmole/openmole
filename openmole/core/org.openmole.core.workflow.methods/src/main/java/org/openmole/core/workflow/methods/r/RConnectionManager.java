/*
 *
 *  Copyright (c) 2007, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.workflow.methods.r;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RConnectionManager {

    private transient RConnection rconnection;
    private static RConnectionManager instance = null;

    private RConnectionManager() {
    }

    public static RConnectionManager GetInstance() {
        if (instance == null) {
            instance = new RConnectionManager();
        }
        return instance;
    }

    public static RConnection getConnection(RConnectionInfo info) {
        if (GetInstance().rconnection == null) {
            try {
                GetInstance().rconnection = new RConnection(info.getRconnectionHost(), info.getRconnectionPort());
            } catch (RserveException ex) {
            	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "R server connection failed. The connection was on " + info.getRconnectionHost() + ":" + info.getRconnectionPort(), ex);
            }
        }
        return GetInstance().rconnection;
    }
}
