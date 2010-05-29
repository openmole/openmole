/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.mole;

import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.execution.IJobStatisticCategorizationStrategy;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.execution.IMoleJobGroupingStrategy;
import org.openmole.commons.tools.pattern.IVisitable;
import org.openmole.core.model.resource.IResource;

public interface IMole extends IVisitable<IGenericTaskCapsule> {

    void run(IEnvironmentSelectionStrategy strategy) throws InternalProcessingError, UserBadDataError, InterruptedException;

    void run() throws UserBadDataError, InternalProcessingError, InterruptedException;

    IMoleExecution start() throws UserBadDataError, InternalProcessingError, InterruptedException;

    IMoleExecution start(IEnvironmentSelectionStrategy strategy) throws UserBadDataError, InternalProcessingError, InterruptedException;

    IMoleExecution createExecution(IContext rootContext, IExecutionContext executionContext) throws UserBadDataError, InternalProcessingError, InterruptedException;

    IMoleExecution createExecution(IEnvironmentSelectionStrategy strategy) throws UserBadDataError, InternalProcessingError, InterruptedException;

    IMoleExecution createExecution() throws UserBadDataError, InternalProcessingError, InterruptedException;

    IGenericTaskCapsule<?, ?> getRoot();

    Iterable<IResource> getAllRessources() throws InternalProcessingError, UserBadDataError;

    void setRoot(IGenericTaskCapsule<?, ?> root);

    public void setMoleJobGroupingStrategy(IGenericTaskCapsule forCapsule, IMoleJobGroupingStrategy strategy);

    public void setJobStatisticCategorizationStrategy(IJobStatisticCategorizationStrategy jobStatisticCategorizationStrategy);
}
