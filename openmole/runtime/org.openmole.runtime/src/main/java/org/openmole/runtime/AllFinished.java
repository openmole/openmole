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

package org.openmole.runtime;

import java.util.concurrent.Semaphore;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.tools.service.Priority;
import org.openmole.runtime.internal.Activator;
import org.openmole.core.model.job.IMoleJob;

/**
 *
 * @author reuillon
 */
public class AllFinished implements IObjectChangedSynchronousListener<IMoleJob> {

    Semaphore allFinished = new Semaphore(0);
    int nbJobs = 0;
    int nbFinished = 0;

    synchronized void registerJob(IMoleJob job) {
        allFinished.drainPermits();
        nbJobs++;
        Activator.getEventDispatcher().registerListener(job, Priority.LOW.getValue(), this, IMoleJob.StateChanged);
    }

    public void waitAllFinished() throws InterruptedException {
        allFinished.acquire();
        allFinished.release();
    }

    @Override
    public synchronized void objectChanged(IMoleJob job) {
        if (job.isFinished()) {
            nbFinished++;
            if (nbFinished >= nbJobs) {
                allFinished.release();
            }
        }
    }
}
