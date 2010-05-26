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

package org.openmole.core.workflow.implementation.mole;

import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.implementation.execution.local.LocalExecutionEnvironmentDescription;
import org.openmole.core.workflow.implementation.internal.Activator;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.mole.IEnvironmentSelectionStrategy;

/**
 *
 * @author reuillon
 */
public class FixedEnvironmentStrategy implements IEnvironmentSelectionStrategy {

    Map<IGenericTaskCapsule, IEnvironment> environments = new HashMap<IGenericTaskCapsule, IEnvironment>();
    IEnvironment defaultEnvironment;

    public FixedEnvironmentStrategy() throws InternalProcessingError {
        defaultEnvironment = Activator.getEnvironmentProvider().getEnvironment(new LocalExecutionEnvironmentDescription());
    }

    @Override
    public IEnvironment selectEnvironment(IGenericTaskCapsule capsule) {
        IEnvironment env = environments.get(capsule);
        if(env != null) return env;
        return defaultEnvironment;
    }

    public void setEnvironment(IGenericTaskCapsule capsule, IEnvironment environment) {
        environments.put(capsule, environment);
    }

  
}
