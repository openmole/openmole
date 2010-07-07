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
package org.openmole.runtime;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;

import org.openmole.core.implementation.tools.FileMigrator;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJobId;
import scala.Tuple2;

public class ContextSaver implements IObjectChangedSynchronousListener<IMoleJob> {

    Collection<File> outFiles = new LinkedList<File>();
    List<Tuple2<IMoleJobId, IContext>> results = new LinkedList<Tuple2<IMoleJobId, IContext>>();

    public ContextSaver() {
        super();
    }

    public Iterable<File> getOutFiles() {
        return outFiles;
    }

    public List<Tuple2<IMoleJobId, IContext>> getResults() {
        return results;
    }

    @Override
    public void objectChanged(IMoleJob job) {
        switch (job.getState()) {
            case COMPLETED:
                Iterable<File> files = FileMigrator.extractFilesFromVariables(job.getContext());

                for (File f : files) {
                    outFiles.add(f);
                }
            case FAILED:
                IContext res = job.getContext();
                res.chRoot();
                results.add(new Tuple2<IMoleJobId, IContext>(job.getId(), res));
        }
    }
}
