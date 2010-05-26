/*
 *  Copyright (C) 2010 Romain euillon
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


package org.openmole.plugin.task.filemanagement;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.mole.IExecutionContext;

public class DeleteFilePrototypeTask extends Task {

	List<IPrototype<File>> toDelete = new LinkedList<IPrototype<File>>();
        List<IPrototype<List<File>>> toDeleteList = new LinkedList<IPrototype<List<File>>>();

	public DeleteFilePrototypeTask(String name) throws UserBadDataError, InternalProcessingError {
		super(name);
	}

	@Override
	protected void process(IContext context, IExecutionContext executionContext, IProgress progress) throws UserBadDataError, InternalProcessingError {
		for(IPrototype<File> p : toDelete) {
			File f = context.getLocalValue(p);
			f.delete();
		}

                for(IPrototype<List<File>> p : toDeleteList){
                    for(File f : context.getLocalValue(p)) {
                        f.delete();
                    }
                }
	}

	public void deleteInputFile(IPrototype<File> prot) {
		toDelete.add(prot);
		super.addInput(prot);
	}

        public void deleteInputFileList(IPrototype<List<File>> prot) {
		toDeleteList.add(prot);
		super.addInput(prot);
	}
}
