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

import org.openmole.core.model.file.IURIFile;
import org.openmole.core.model.message.IExecutionMessage;
import org.openmole.core.model.message.IFileMessage;
import org.openmole.core.model.message.IReplicatedFile;

/**
 *
 * @author reuillon
 */
public class ExecutionMessage implements IExecutionMessage {

    final Iterable<IReplicatedFile> plugins;
    final Iterable<IReplicatedFile> files;
    final IFileMessage jobForRuntime;
    final IURIFile communicationDir;

    public ExecutionMessage(Iterable<IReplicatedFile> plugins, Iterable<IReplicatedFile> files, IFileMessage jobForRuntime, IURIFile communicationDir) {
        this.plugins = plugins;
        this.files = files;
        this.jobForRuntime = jobForRuntime;
        this.communicationDir = communicationDir;
    }

    @Override
    public Iterable<IReplicatedFile> getPlugins() {
        return plugins;
    }

    @Override
    public IFileMessage getJobForRuntimeURI() {
        return jobForRuntime;
    }

    @Override
    public Iterable<IReplicatedFile> getFiles() {
        return files;
    }

    @Override
    public IURIFile getCommunicationDir() {
        return communicationDir;
    }

}
