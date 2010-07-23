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
package org.openmole.core.implementation.execution.batch;

import org.openmole.core.model.execution.batch.IBatchStorage;
import org.openmole.core.model.execution.batch.IRuntime;
import org.openmole.core.model.file.IURIFile;

/**
 *
 * @author reuillon
 */
public class CopyToEnvironmentResult {

    public CopyToEnvironmentResult(IBatchStorage communicationStorage, IURIFile communicationDir, IURIFile inputFile, IURIFile outputFile, IRuntime runtime) {
        this.communicationStorage = communicationStorage;
        this.communicationDir = communicationDir;
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.runtime = runtime;
    }
    
    final public IBatchStorage communicationStorage;
    final public IURIFile communicationDir;
    final public IURIFile inputFile;
    final public IURIFile outputFile;
    final public IRuntime runtime;
}
