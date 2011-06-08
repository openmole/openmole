/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.domain.collection;

import scala.collection.Iterator;
import java.util.Arrays;
import java.util.List;
import org.openmole.core.model.data.IContext;
import org.openmole.core.model.domain.IFiniteDomain;
import scala.collection.Iterable;
import static scala.collection.JavaConversions.*;

/**
 *
 * @author reuillon
 */
public class ValueSetDomain<T> implements IFiniteDomain<T> {

    private final List<T> values;

    public ValueSetDomain(T... values) {
        this.values = Arrays.asList(values);
    }

    public Iterator<T> iterator(IContext context) {
        return computeValues(context).iterator();
    }

    public Iterable<T> computeValues(IContext context) {
       return asScalaBuffer(values);
    }
}
