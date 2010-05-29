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
package org.openmole.core.implementation.execution.batch;

import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchService;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.core.model.execution.batch.IFailureControl;
import org.openmole.core.model.execution.batch.IUsageControl;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class BatchService implements IBatchService {

    private IBatchServiceDescription description;
    private IBatchEnvironment executionEnvironment;

   // private IBatchServiceGroup<?> group;

    public BatchService(IBatchEnvironment executionEnvironment, IBatchServiceDescription description, IUsageControl usageControl, IFailureControl failureControl) {

        if(!Activator.getBatchRessourceControl().contains(description)) {
            Activator.getBatchRessourceControl().registerRessouce(description, usageControl, failureControl);
        } else {
            Activator.getBatchRessourceControl().reinitFailure(description, failureControl);
        }

        this.description = description;
        this.executionEnvironment = executionEnvironment;
    }

    @Override
    public IBatchServiceDescription getDescription() {
        return description;
    }

   
    @Override
    public IBatchEnvironment<?, ?> getExecutionEnvironment() {
        return executionEnvironment;
    }

    @Override
    public String toString() {
        return getDescription().toString();
    }
   /* @Override
    public void setGroup(IBatchServiceGroup<?> group) {
        this.group = group;
    }*/
}
