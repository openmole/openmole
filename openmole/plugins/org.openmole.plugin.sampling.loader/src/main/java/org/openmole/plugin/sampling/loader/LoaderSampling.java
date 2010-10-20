/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.plugin.sampling.loader;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.implementation.observer.Saver;
import org.openmole.core.implementation.sampling.Sample;
import org.openmole.core.implementation.sampling.Values;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.sampling.ISample;
import org.openmole.core.model.sampling.ISampling;
import org.openmole.core.model.sampling.IValues;
import org.openmole.plugin.sampling.loader.internal.Activator;

/**
 *
 * @author reuillon
 */
public class LoaderSampling implements ISampling {
    
    final File dir;

    public LoaderSampling(File dir) {
        this.dir = dir;
    }
    
    @Override
    public ISample build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        Collection<IValues> values = new LinkedList<IValues>();
        HashToFile hashToFile = new HashToFile(new File(dir, Saver.FILES));
        
        for(File ctxFile: new File(dir, Saver.CONTEXTS).listFiles()) {
            IContext ctx = Activator.getSerializer().deserializeReplacePathHash(ctxFile, hashToFile);
            Values val = new Values();
            for(IVariable v: ctx) {
                val.setValue(v.getPrototype(), v.getValue());
            }
            values.add(val);
        }
        
        return new Sample(values);
    }

}
