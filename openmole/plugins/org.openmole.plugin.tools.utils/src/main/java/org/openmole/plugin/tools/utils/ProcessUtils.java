/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.tools.utils;

import java.io.PrintStream;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;

public class ProcessUtils {

    static ProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();

    public static int executeProcess(Process process, PrintStream out, PrintStream err) throws InterruptedException {
        PumpStreamHandler pump = new PumpStreamHandler(out, err);

        pump.setProcessOutputStream(process.getInputStream());
        pump.setProcessErrorStream(process.getErrorStream());

        processDestroyer.add(process);
        try {
            pump.start();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                process.destroy();
                throw e;
            } finally {
                pump.stop();
            }
        } finally {
            processDestroyer.remove(process);
        }
        return process.exitValue();
    }
}
