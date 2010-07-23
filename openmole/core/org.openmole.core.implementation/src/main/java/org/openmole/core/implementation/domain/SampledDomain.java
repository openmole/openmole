/*
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.core.implementation.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.domain.IDomain;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;


public class SampledDomain<T> extends FiniteDomain<T> {

    private int size;
    private IDomain<? extends T> domain;

    public SampledDomain(IDomain<? extends T> domain, int size) {
        this.domain = domain;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public  List<T> computeValues(IContext global, IContext context) throws UserBadDataError, InternalProcessingError {
        List<T> values = new ArrayList<T>(size);
        Iterator<? extends T> it = domain.iterator(global, context);
        int i = 0;

        while (i < size && it.hasNext()) {
            values.add(it.next());
            i++;
        }
        return values;
    }

  

}
