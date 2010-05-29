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

package org.openmole.core.implementation.task;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.data.IData;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;

public class InputToGlobalTask extends Task {

	public InputToGlobalTask(String name) throws UserBadDataError,
			InternalProcessingError {
		super(name);
	}

	public InputToGlobalTask(String name, IData... data) throws UserBadDataError,
			InternalProcessingError {
		super(name);
                
                for(IData d: data) {
                    addInput(d);
                }
	}

        public InputToGlobalTask(String name, IPrototype... prototypes) throws UserBadDataError,
			InternalProcessingError {
		super(name);

                for(IPrototype prototype: prototypes) {
                    addInput(prototype);
                }
	}

	@Override
	protected void process(IContext context, IExecutionContext executionContext, IProgress progress)
			throws UserBadDataError, InternalProcessingError {
		for(IData data : getInput()) {
                        IPrototype p = data.getPrototype();
			if(!data.isOptional() || data.isOptional() && context.contains(p)) {
				context.putGlobalVariable(p, context.getLocalValue(p));
			}
		}
		
	}

}
