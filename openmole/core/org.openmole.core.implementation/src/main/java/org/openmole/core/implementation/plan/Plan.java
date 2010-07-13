/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import org.openmole.core.model.plan.IPlan;
import java.util.List;

import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.resource.IResource;
import org.openmole.core.model.task.annotations.Resource;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.core.implementation.tools.MarkedFieldFinder.*;

public abstract class Plan<T extends IFactor<?, ?>> implements IPlan {

    List<T> factors = new ArrayList<T>();

    public Plan() {
    }

    public Plan(T... factors) {
        for (T f : factors) {
            addFactor(f);
        }
    }

    public void addFactor(T e) {
        factors.add(e);
    }

    public List<T> getFactors() {
        return factors;
    }

}
