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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.sampler.Sample;
import org.openmole.core.implementation.sampler.Values;
import org.openmole.core.model.sampler.IFactor;
import org.openmole.core.model.sampler.ISample;
import org.openmole.core.model.sampler.ISampler;
import org.openmole.core.model.sampler.IValues;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class ZipSamplerCombinasion implements ISamplerCombinasion {

    final private ISampler reference;
    final private Collection<ISampler> samplers = new LinkedList<ISampler>();

    public ZipSamplerCombinasion(ISampler refPlan) {
        this.reference = refPlan;
    }

    public ZipSamplerCombinasion(ISampler reference, IFactor<Object,?>... factors) {
        this.reference = reference;

        for(IFactor factor: factors) {
            samplers.add(new FactorSamplerAdapter(factor));
        }
    }

    public ZipSamplerCombinasion(IFactor<Object,?> reference, IFactor<Object,?>... factors) {
        this(new FactorSamplerAdapter(reference), factors);
    }

    public ZipSamplerCombinasion(ISampler reference, ISampler... samplers) {
        this.reference = reference;

        for(ISampler sampler: samplers) {
            this.samplers.add(sampler);
        }
    }

    @Override
    public ISample build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {

        /* Compute plans */
        Collection<Iterator<IValues>> cachedSample = new ArrayList<Iterator<IValues>>(samplers.size());

        for(ISampler otherSampler: getSamplers()) {
            cachedSample.add(otherSampler.build(global, context).iterator());
        }

        /* Compose plans */
        Collection<IValues> factorValuesCollection = new LinkedList<IValues>();

        Iterator<IValues> valuesIterator = reference.build(global, context).iterator();
        boolean oneFinished = false;

        while(valuesIterator.hasNext() && !oneFinished) {
            Values values = new Values();

            for(Iterator<IValues> it: cachedSample) {
                if(!it.hasNext()) {
                    oneFinished = true;
                    break;
                }
                values.addAll(it.next());
            }

            if(!oneFinished) {
                values.addAll(valuesIterator.next());
                factorValuesCollection.add(values);
            }
        }

        return new Sample(factorValuesCollection);
    }


    public Collection<ISampler> getSamplers() {
        return samplers;
    }

    public ISampler getReferenceSampler() {
        return reference;
    }



}
