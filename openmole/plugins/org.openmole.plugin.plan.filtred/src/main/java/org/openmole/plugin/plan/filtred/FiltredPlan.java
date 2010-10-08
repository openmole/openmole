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

package org.openmole.plugin.plan.filtred;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.openmole.core.implementation.plan.ExploredPlan;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.core.model.plan.IPlan;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FiltredPlan implements IPlan {

    final IPlan toFilter;
    List<IFilter> filters = new LinkedList<IFilter>();

    public FiltredPlan(IPlan toFilter) {
        this.toFilter = toFilter;
    }

    public void addFilter(IFilter filter) {
        filters.add(filter);
    }

    @Override
    public IExploredPlan build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        IExploredPlan exploredPlan = toFilter.build(global, context);
        Collection<IFactorValues> ret = new LinkedList<IFactorValues>();
        Iterator<IFactorValues> it = exploredPlan.iterator();

        while(it.hasNext()) {
            IFactorValues values = it.next();
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

        return new ExploredPlan(ret);
    }

}
