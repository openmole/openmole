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

package org.openmole.plugin.sampling.combine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.sampling.Sample;
import org.openmole.core.implementation.sampling.Values;
import org.openmole.core.model.sampling.IFactor;
import org.openmole.core.model.sampling.ISample;
import org.openmole.core.model.sampling.ISampling;
import org.openmole.core.model.sampling.IValues;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class ZipSamplingCombinasion implements ISamplingCombinasion {

    final private ISampling reference;
    final private Collection<ISampling> samplers = new LinkedList<ISampling>();

    public ZipSamplingCombinasion(ISampling refPlan) {
        this.reference = refPlan;
    }

    public ZipSamplingCombinasion(ISampling reference, IFactor<Object,?>... factors) {
        this.reference = reference;

        for(IFactor factor: factors) {
            samplers.add(new FactorSamplingAdapter(factor));
        }
    }

    public ZipSamplingCombinasion(IFactor<Object,?> reference, IFactor<Object,?>... factors) {
        this(new FactorSamplingAdapter(reference), factors);
    }

    public ZipSamplingCombinasion(ISampling reference, ISampling... samplers) {
        this.reference = reference;

        for(ISampling sampler: samplers) {
            this.samplers.add(sampler);
        }
    }

    @Override
    public ISample build(IContext global, IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {

        /* Compute plans */
        Collection<Iterator<IValues>> cachedSample = new ArrayList<Iterator<IValues>>(samplers.size());

        for(ISampling otherSampler: getSamplers()) {
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


    public Collection<ISampling> getSamplers() {
        return samplers;
    }

    public ISampling getReferenceSampler() {
        return reference;
    }



}
