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

package org.openmole.core.workflow.implementation.execution.batch;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.batch.IBatchJob;

public class JobKiller implements Runnable {

	IBatchJob job;
	

	public JobKiller(IBatchJob job) {
		super();
		this.job = job;
	}

	@Override
	public void run() {
		try {
			job.kill();
		} catch (InternalProcessingError e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,"Could not kill job " + job.toString(), e);
		} catch (UserBadDataError e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,"Could not kill job " + job.toString(), e);
		}catch (InterruptedException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,"Could not kill job " + job.toString(), e);
		} 
	}

}
