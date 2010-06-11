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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.openmole.core.implementation.plan.ExploredPlan;
import org.openmole.core.implementation.plan.FactorsValues;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.core.model.plan.IPlan;
import org.openmole.core.model.resource.IResource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.plan.IFactor;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class OneOfEachPlanCombinasion implements IPlanCombinasion {

    final IPlan referencePlan;
    Collection<IPlan> plans = new LinkedList<IPlan>();

    public OneOfEachPlanCombinasion(IPlan refPlan) {
        this.referencePlan = refPlan;
    }

    public OneOfEachPlanCombinasion(IPlan refPlan, IFactor<Object,?>... factors) {
        this.referencePlan = refPlan;

        for(IFactor factor: factors) {
            plans.add(new OneFactorPlan(factor));
        }
    }

    public OneOfEachPlanCombinasion(IFactor<Object,?> refFactor, IFactor<Object,?>... factors) {
        this(new OneFactorPlan(refFactor), factors);
    }

    public OneOfEachPlanCombinasion(IPlan refPlan, IPlan... plans) {
        this.referencePlan = refPlan;

        for(IPlan plan: plans) {
            this.plans.add(plan);
        }
    }

    @Override
    public IExploredPlan build(IContext context) throws InternalProcessingError, UserBadDataError, InterruptedException {

        /* Compute plans */
        Collection<Iterator<IFactorValues>> cachedExploredPlans = new ArrayList<Iterator<IFactorValues>>(plans.size());

        for(IPlan otherPlan: getPlans()) {
            cachedExploredPlans.add(otherPlan.build(context).iterator());
        }

        /* Compose plans */
        Collection<IFactorValues> factorValuesCollection = new LinkedList<IFactorValues>();

        Iterator<IFactorValues> valuesIterator = referencePlan.build(context).iterator();
        boolean oneFinished = false;

        while(valuesIterator.hasNext() && !oneFinished) {
            FactorsValues values = new FactorsValues();

            for(Iterator<IFactorValues> it: cachedExploredPlans) {
                if(!it.hasNext()) {
                    oneFinished = true;
                    break;
                }
                addAll(it.next(), values);
            }

            if(!oneFinished) {
                addAll(valuesIterator.next(), values);
                factorValuesCollection.add(values);
            }
        }

        return new ExploredPlan(factorValuesCollection);
    }

    void addAll(IFactorValues from, FactorsValues to) {
        for(String name: from.getNames()) {
            to.setValue(name, from.getValue(name));
        }
    }

    @Override
    public Iterable<IResource> getResources() throws InternalProcessingError, UserBadDataError {
        Collection<IResource> ret = new LinkedList<IResource>();


        for(IResource resource: getReferencePlan().getResources()) {
            ret.add(resource);
        }

        for(IPlan plan: getPlans()) {
            for(IResource resource: plan.getResources()) {
                ret.add(resource);
            }
        }

        return ret;
    }

    public Collection<IPlan> getPlans() {
        return plans;
    }

    public IPlan getReferencePlan() {
        return referencePlan;
    }

}
