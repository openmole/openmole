/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.plan.combine;

import java.util.Iterator;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.core.model.plan.IPlan;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.plan.FactorsValues;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class OneFactorPlan implements IPlan {

    final IFactor factor;

    public OneFactorPlan(IFactor factor) {
        this.factor = factor;
    }

    @Override
    public IExploredPlan build(final IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        return new IExploredPlan() {

            @Override
            public Iterator<IFactorValues> iterator() throws UserBadDataError, InternalProcessingError {
                return new Iterator<IFactorValues>() {

                    final Iterator iterator = factor.getDomain().iterator(context);

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public IFactorValues next() {
                        FactorsValues values = new FactorsValues();
                        values.setValue(factor.getPrototype(), iterator.next());
                        return values;
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    @Override
    public Iterable<IResource> getResources() throws InternalProcessingError, UserBadDataError {
        return factor.getResources();
    }

}
