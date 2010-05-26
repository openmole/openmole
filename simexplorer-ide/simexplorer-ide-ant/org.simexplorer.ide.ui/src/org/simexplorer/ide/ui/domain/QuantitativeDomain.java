/*
 *  Copyright © 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ide.ui.domain;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.plugin.domain.interval.RangeDouble;
import org.openmole.plugin.domain.interval.RangeInteger;
import org.openmole.plugin.domain.relativerange.RelativeRangeInteger;
import org.openmole.plugin.domain.relativerange.RelativeRangeDouble;
import org.openmole.core.workflow.implementation.domain.FiniteDomain;
import org.openmole.core.workflow.model.job.IContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openmole.core.workflow.model.domain.IDomain;

/**
 *
 * @author thierry
 */
public class QuantitativeDomain extends FiniteDomain {

    private static Map<Class, List<Class<? extends IDomain>>> factorDomains;

    public QuantitativeDomain() {
        factorDomains = new HashMap<Class, List<Class<? extends IDomain>>>();
        addInMap(factorDomains, Integer.class, RangeInteger.class, RelativeRangeInteger.class);
        addInMap(factorDomains, Double.class, RangeDouble.class, RelativeRangeDouble.class);
    }

    private <T, U> void addInMap(Map<T, List<Class<? extends U>>> map, T key, Class<? extends U>... values) {
        List<Class<? extends U>> mapResult = map.get(key);
        if (mapResult == null) {
            mapResult = new ArrayList<Class<? extends U>>(values.length);
            map.put(key, mapResult);
        }
        for (Class<? extends U> value : values) {
            mapResult.add(value);
        }
    }

    public static Map<Class, List<Class<? extends IDomain>>> getFactorDomains() {
        return factorDomains;
    }

    public static Class getDomainObjectType(Class class1) {
        for (Class c : factorDomains.keySet()) {
            for (Class<? extends IDomain> cd : factorDomains.get(c)) {
                if (class1 == cd) {
                    return c;
                }
            }
        }
        return Object.class;
    }

    @Override
    public List computeValues(IContext context) throws InternalProcessingError, UserBadDataError {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
