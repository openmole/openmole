/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;

import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJobId;

public class ContextSaver implements IObjectChangedSynchronousListener<IMoleJob> {

    Map<IMoleJobId, IContext> results = Collections.synchronizedMap(new TreeMap<IMoleJobId, IContext>());

    public ContextSaver() {
        super();
    }

    public Map<IMoleJobId, IContext> getResults() {
        return results;
    }

    @Override
    public void objectChanged(IMoleJob job) {
        switch (job.getState()) {
            case COMPLETED:
            case FAILED:
                IContext res = job.getContext();
                results.put(job.getId(), res);
        }
    }
}
