/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.sampling.filter;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.sampling.Sample;
import org.openmole.core.model.sampling.ISample;
import org.openmole.core.model.sampling.ISampling;
import org.openmole.core.model.sampling.IValues;


/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FiltredSampling implements ISampling {

    final ISampling toFilter;
    List<IFilter> filters = new LinkedList<IFilter>();

    public FiltredSampling(ISampling toFilter) {
        this.toFilter = toFilter;
    }

    public void addFilter(IFilter filter) {
        filters.add(filter);
    }

    @Override
    public ISample build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        ISample exploredPlan = toFilter.build(global, context);
        Collection<IValues> ret = new LinkedList<IValues>();
        Iterator<IValues> it = exploredPlan.iterator();

        while(it.hasNext()) {
            IValues values = it.next();
            boolean accepted = true;

            for(IFilter filter: filters) {
                if(!filter.accept(values)) {
                    accepted = false;
                    break;
                }
            }

            if(accepted) {
                ret.add(values);
            }
        }

        return new Sample(ret);
    }

}
