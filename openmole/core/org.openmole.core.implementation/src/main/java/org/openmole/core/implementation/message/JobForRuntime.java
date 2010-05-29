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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.workflow.model.message.IJobForRuntime;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.message.IReplicatedFile;

public class JobForRuntime extends RuntimeMessage implements IJobForRuntime {

    List<IMoleJob> moleJobs = new LinkedList<IMoleJob>();
    IURIFile communicationDir;
    List<IReplicatedFile> consumedFiles = new LinkedList<IReplicatedFile>();

    public JobForRuntime(IURIFile communicationDir) {
        super();
        this.communicationDir = communicationDir;
    }

    @Override
    public void addMoleJob(IMoleJob moleJob) {
        moleJobs.add(moleJob);
    }

    @Override
    public IURIFile getCommunicationDir() {
        return communicationDir;
    }

    @Override
    public void addConsumedFile(IReplicatedFile repli) {
        consumedFiles.add(repli);
    }

    @Override
    public List<IReplicatedFile> getConsumedFiles() {
        return consumedFiles;
    }

    @Override
    public synchronized void addConsumedFiles(List<IReplicatedFile> repli) {
        consumedFiles.addAll(repli);
    }

  
    @Override
    public Collection<IMoleJob> getMoleJobs() {
        return moleJobs;
    }

}
