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

package org.openmole.plugin.domain.collection;

import java.util.Arrays;
import java.util.Iterator;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.domain.IDomain;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class ValueSetDomain<T> implements IDomain<T> {
    
    final Iterable<T> values;
    
    public ValueSetDomain(T ...values) {
        this.values = Arrays.asList(values);
    }

    @Override
    public Iterator<? extends T> iterator(IContext ic, IContext ic1) throws UserBadDataError, InternalProcessingError {
        return values.iterator();
    }

}
