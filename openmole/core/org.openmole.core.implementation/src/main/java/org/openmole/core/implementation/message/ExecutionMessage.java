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
package org.openmole.core.implementation.message;

import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IReplicatedFile;

/**
 *
 * @author reuillon
 */
public class ExecutionMessage implements IExecutionMessage {

    Iterable<IReplicatedFile> plugins;
    Duo<IURIFile, IHash> jobForRuntime;

    public ExecutionMessage(Iterable<IReplicatedFile> plugins, Duo<IURIFile, IHash> jobForRuntime) {
        this.plugins = plugins;
        this.jobForRuntime = jobForRuntime;
    }

    @Override
    public Iterable<IReplicatedFile> getPlugins() {
        return plugins;
    }

    @Override
    public Duo<IURIFile, IHash> getJobForRuntimeURI() {
        return jobForRuntime;
    }

}
