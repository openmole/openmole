/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.sampler.combine;

import java.util.Iterator;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.sampler.Values;
import org.openmole.core.model.sampler.IFactor;
import org.openmole.core.model.sampler.ISample;
import org.openmole.core.model.sampler.ISampler;
import org.openmole.core.model.sampler.IValues;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class FactorSamplerAdapter implements ISampler {

    final IFactor factor;

    public FactorSamplerAdapter(IFactor factor) {
        this.factor = factor;
    }

    @Override
    public ISample build(final IContext global, final IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {
        return new ISample() {

            @Override
            public Iterator<IValues> iterator() throws UserBadDataError, InternalProcessingError {
                return new Iterator<IValues>() {

                    final Iterator iterator = factor.getDomain().iterator(global, context);

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public IValues next() {
                        Values values = new Values();
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

}
