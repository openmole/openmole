/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.domain.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.domain.IDomain;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class IterableDomain<T> implements IDomain<T>{

    final Iterable<? extends T> iterable;

    public IterableDomain(Iterable<? extends T> iterable) {
        this.iterable = iterable;
    }

    public IterableDomain(T... elements) {
        List<T> list = new ArrayList<T>(elements.length);
        for(T element: elements) {
            list.add(element);
        }
        this.iterable = list;
    }

    @Override
    public Iterator<? extends T> iterator(IContext global, IContext context) throws UserBadDataError, InternalProcessingError {
        return iterable.iterator();
    }

}
