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

package org.openmole.core.workflow.methods.task;

import java.io.File;
import java.io.IOException;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.methods.internal.Activator;
import org.openmole.core.workflow.methods.resource.OctaveRessource;
import org.openmole.core.workflow.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.core.workflow.model.task.annotations.Resource;


public class OctaveTask /*extends ExternalTask */{

	/*String src;

	@Resource
	OctaveRessource octave;

	public OctaveTask(String name, String src, OctaveRessource octave) throws UserBadDataError,
	InternalProcessingError {
		super(name);
		this.src = src;
		this.octave = octave;
	}

	@Override
	protected void process(IContext context, IExecutionContext executionContext, IProgress progress)
	throws UserBadDataError, InternalProcessingError {

		try {
	
			File tmpDir = Activator.getWorkspace().newTmpDir("octaveTask");
			
			prepareInputFiles(context, progress, tmpDir);
	
			octave.execute(tmpDir, VariableExpansion.getInstance().expandData(context, src));

			fetchOutputFiles(context, progress, tmpDir);
			
		} catch (IOException e) {
			throw new InternalProcessingError(e);
		}
	}*/


}
