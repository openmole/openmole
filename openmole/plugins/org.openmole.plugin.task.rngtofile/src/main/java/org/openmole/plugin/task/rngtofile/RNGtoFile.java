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

import java.io.FileOutputStream;
import java.io.OutputStream;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.implementation.task.Task;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.tools.distrng.data.MT19937v32Data;
import org.openmole.tools.distrng.rng.RNG;
import org.openmole.tools.distrng.serializer.DataSerializer;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class RNGtoFile extends Task{

    DataSerializer<RNGData> serializer;
    OutputStream fileStream;
    Prototype<? extends RNG> rng;
    
    public RNGtoFile(String name,
                     DataSerializer<RNGData> serializer,
                     Prototype<? extends RNG> rng,
                     String filePath) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.serializer = serializer;        
        this.fileStream = new FileOutputStream(filePath);
        this.rng = rng;
    }
    
    

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError, InterruptedException {
        //MT19937v32Data data = (MT19937v32Data) context.getValue(rng).getData();

        serializer.toString((MT19937v32Data) context.getValue(rng).getData(),fileStream);
        
        /*for (Integer r : data.state){
            
            System.out.println("state : " + r.toString());
        }*/
        
        // serializer.saveFile(this, filePath);
    }
    
}
