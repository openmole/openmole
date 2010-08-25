/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.rngtofile;

import java.util.LinkedList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.task.Task;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.tools.distrng.prng.PRNG;
import org.openmole.tools.distrng.serializer.Serializer;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class RNGtoFile extends Task{

    Serializer serializer;
    String filePath;
    List<Prototype<PRNG>> statusList = new LinkedList<Prototype<PRNG>>();
    
    public RNGtoFile(String name,
                           Serializer serializer,
                           String filePath) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.serializer = serializer;
        this.filePath = filePath;
    }
    
    

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {
       // serializer.saveFile(this, filePath);
    }
    
    public void addSatus(){
        
    }
    
}
