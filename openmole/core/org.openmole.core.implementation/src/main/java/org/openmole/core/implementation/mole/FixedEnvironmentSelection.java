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

package org.openmole.core.implementation.mole;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.mole.IEnvironmentSelection;

/**
 *
 * @author reuillon
 */
public class FixedEnvironmentSelection implements IEnvironmentSelection {
   
    static final IEnvironmentSelection EMPTY_SELECTION = new FixedEnvironmentSelection(Collections.EMPTY_MAP);
    
    final Map<IGenericTaskCapsule, IEnvironment> environments;

    public FixedEnvironmentSelection() {
        this(new HashMap<IGenericTaskCapsule, IEnvironment>());
    }
    
    private FixedEnvironmentSelection(Map<IGenericTaskCapsule, IEnvironment> environments) {
        this.environments = environments;
    }

    @Override
    public IEnvironment selectEnvironment(IGenericTaskCapsule capsule) {
        return environments.get(capsule);
    }

    public void setEnvironment(IGenericTaskCapsule capsule, IEnvironment environment) {
        environments.put(capsule, environment);
    }

  
}
