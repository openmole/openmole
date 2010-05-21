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
package org.openmole.plugin.environmentprovider.glite.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.misc.updater.IUpdatable;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class WaitingThreadInterrupter implements IUpdatable {

    @Override
    public void update() throws InternalProcessingError, UserBadDataError, InterruptedException {
        /* -- Fixing bug of cog leaving inactive thread
         * this code should be removed when cog bug has been fixed
         * https://bugzilla.mcs.anl.gov/globus/show_bug.cgi?id=6902
         * --
         */

        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = group.getParent();

        while (parent != null) {
            group = parent;
            parent = group.getParent();
        }

        Thread threads[] = new Thread[nbThread(group)];
        group.enumerate(threads, true);

        int nbKilledThreads = 0;

        for (Thread t : threads) {
            if (t != null) {
                StackTraceElement[] stackTrace = t.getStackTrace();
                if (stackTrace.length >= 3) {
                    if (stackTrace[2].toString().equals("org.globus.ftp.dc.TaskThread$Buffer.get(TaskThread.java:122)") && stackTrace[1].toString().equals("java.lang.Object.wait(Object.java:485)")) {
                        t.interrupt();
                        nbKilledThreads++;
                    }
                }
            }
        }

        //Logger.getLogger(WaitingThreadInterrupter.class.getName()).log(Level.INFO, nbKilledThreads + " interrupted threads.");

        /*-- end of bug fix
         *--
         */
    }

    int nbThread(ThreadGroup group) {
        int totThread = 0;

        ThreadGroup[] grps = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(grps);


        for (ThreadGroup gpr : grps) {
            totThread += nbThread(gpr);
        }

        return totThread += group.activeCount();
    }

    @Override
    public long getUpdateInterval() {
        return 60 * 1000;
    }
}
