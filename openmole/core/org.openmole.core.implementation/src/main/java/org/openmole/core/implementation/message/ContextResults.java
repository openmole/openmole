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

package org.openmole.core.implementation.message;

import java.util.Map;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.message.IContextResults;

/**
 *
 * @author reuillon
 */
public class ContextResults implements IContextResults {

    final Map<IMoleJobId, IContext> results;

    public ContextResults(Map<IMoleJobId, IContext> results) {
        this.results = results;
    }

    @Override
    public boolean containsResultForJob(IMoleJobId jobId) {
        return results.containsKey(jobId);
    }

    @Override
    public IContext getContextForJob(IMoleJobId jobId) {
        return results.get(jobId);
    }
      
}
