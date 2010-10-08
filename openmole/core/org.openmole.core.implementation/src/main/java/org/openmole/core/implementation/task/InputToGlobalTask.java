/*
 *  Copyright (C) 2010 Romain Reuillon
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
package org.openmole.core.implementation.task;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;

public class InputToGlobalTask extends Task {

    public InputToGlobalTask(String name) throws UserBadDataError,
            InternalProcessingError {
        super(name);
    }

    public InputToGlobalTask(String name, IData... data) throws UserBadDataError,
            InternalProcessingError {
        super(name);

        for (IData d : data) {
            addInput(d);
        }
    }

    public InputToGlobalTask(String name, IPrototype... prototypes) throws UserBadDataError,
            InternalProcessingError {
        super(name);

        for (IPrototype prototype : prototypes) {
            addInput(prototype);
        }
    }

    @Override
    protected void process(IContext global, IContext context, IProgress progress)
            throws UserBadDataError, InternalProcessingError {
        for (IData data : getUserInput()) {
            IPrototype p = data.getPrototype();
            if (!data.getMode().isOptional() || data.getMode().isOptional() && context.contains(p)) {
                global.putVariable(p, context.getValue(p));
            }
        }
    }
}
