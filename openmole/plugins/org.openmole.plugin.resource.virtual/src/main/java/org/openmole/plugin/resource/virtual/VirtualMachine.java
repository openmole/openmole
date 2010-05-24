/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.resource.virtual;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class VirtualMachine  implements IVirtualMachine {

    private final String host;
    private final int port;
    private final Process process;

    public VirtualMachine(String host, int port, Process process) {
        this.host = host;
        this.port = port;
        this.process = process;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void shutdown() {
        process.destroy();
    }

    
}
