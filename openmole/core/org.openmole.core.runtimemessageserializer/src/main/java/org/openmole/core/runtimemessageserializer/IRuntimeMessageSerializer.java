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

package org.openmole.core.runtimemessageserializer;

import java.io.File;
import java.io.IOException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IJobForRuntime;
import org.openmole.core.model.message.IRuntimeResult;

public interface IRuntimeMessageSerializer {

    IJobForRuntime loadJobForRuntime(File file) throws IOException;
    Iterable<Class> saveJobForRuntime(IJobForRuntime job, File file) throws InternalProcessingError, IOException;
    void saveExecutionMessage(IExecutionMessage job, File file) throws IOException;
    IExecutionMessage loadExecutionMessage(File file) throws IOException;

    IRuntimeResult loadJarRuntimeResult(File file) throws IOException;
    void saveRuntimeResult(IRuntimeResult job, File file) throws IOException;
}
