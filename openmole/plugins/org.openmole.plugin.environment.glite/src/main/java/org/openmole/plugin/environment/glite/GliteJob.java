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

package org.openmole.plugin.environment.glite;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.plugin.environment.jsaga.JSAGAJob;

/**
 *
 * @author reuillon
 */
public class GliteJob extends JSAGAJob {

    final static Logger LOGGER = Logger.getLogger(GliteJob.class.getName());
    
    final long proxyExpired;
    
    public GliteJob(String jobId, GliteJobService jobService, long proxyExpired) throws InternalProcessingError {
        super(jobId, jobService);
        this.proxyExpired = proxyExpired;
        LOGGER.log(Level.FINE, "Job will be killed if not executed before {0}", proxyExpired);
    }

    @Override
    public ExecutionState updateState() throws InternalProcessingError {
        if(proxyExpired < System.currentTimeMillis()) throw new InternalProcessingError("Proxy for this job has expired.");
        return super.updateState();
    }

    
    
    
}
