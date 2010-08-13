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

import org.openmole.core.model.message.IJobForRuntime;
import org.openmole.core.model.job.IMoleJob;

public class JobForRuntime extends RuntimeMessage implements IJobForRuntime {

    final Iterable<IMoleJob> moleJobs;

    public JobForRuntime(Iterable<IMoleJob> moleJobs) {
        super();
        this.moleJobs = moleJobs;
    }
  
    @Override
    public Iterable<IMoleJob> getMoleJobs() {
        return moleJobs;
    }

}
