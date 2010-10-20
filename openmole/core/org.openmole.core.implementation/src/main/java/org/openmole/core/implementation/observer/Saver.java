/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.observer;

import java.util.Collection;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.observer.ISaver;
import scala.Tuple2;

import static org.openmole.commons.tools.io.FileUtil.*;

/**
 *
 * @author reuillon
 */
public class Saver implements ISaver, IMoleExecutionObserver {

    final static public String FILES = "files";
    final static public String CONTEXTS = "contexts";
    final File dir;

    public Saver(IMoleExecution moleExection, IGenericTaskCapsule taskCapsule, File dir) {
        new MoleExecutionObserverAdapter(moleExection, this);
        this.dir = dir;
        dir.mkdirs();
        new File(dir, FILES).mkdir();
        new File(dir, CONTEXTS).mkdir();
    }

    public Saver(IMoleExecution moleExection, IGenericTaskCapsule taskCapsule, String dir) {
        this(moleExection, taskCapsule, new File(dir));
    }

    @Override
    public synchronized void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
/*      Set<String> filter = new TreeSet<String>();
        
        for(IData data: moleJob.getTask().getOutput()) {
        if(data.getMode().isSystem()) filter.add(data.getPrototype().getName());
        }
        
        IContext context = new Context();
        
        for(IVariable variable: moleJob.getContext()) {
        if(!filter.contains(variable.getPrototype().getName())) context.putVariable(variable);
        }*/

        Tuple2<Map<File, IHash>, Collection<Class>> serialization = Activator.getSerializer().serializeFilePathAsHashGetPluginClassAndFiles(moleJob.getContext(), new File(dir, CONTEXTS));

        try {
            File files = new File(dir, FILES);
            for (Map.Entry<File, IHash> f : serialization._1.entrySet()) {
                File file = new File(files, f.getValue().toString());
                if (!file.exists()) {
                    copy(f.getKey(), file);
                }
            }
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    @Override
    public void moleExecutionStarting() throws InternalProcessingError, UserBadDataError {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void moleExecutionFinished() throws InternalProcessingError, UserBadDataError {
    }
}
