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

package org.openmole.core.implementation.domain;

import java.util.Iterator;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class IteratorDomain<T> extends Domain<T> {

    final Iterator<? extends T> iterator;

    public IteratorDomain(Iterator<? extends T> iterator) {
        this.iterator = iterator;
    }

    public IteratorDomain(Iterable<? extends T> iterable) {
        this.iterator = iterable.iterator();
    }


    @Override
    public Iterator<? extends T> iterator(IContext context) throws UserBadDataError, InternalProcessingError {
        return iterator;
    }



}
