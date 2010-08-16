/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.commons.tools.stat;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FixedSizeList<T> {

    final int size;
    final LinkedList<T> list = new LinkedList<T>();

    public FixedSizeList(int size) {
        super();
        this.size = size;
    }

    public synchronized void add(T nval) {
        if (list.size() < size) {
            list.offer(nval);
        } else {
            list.remove();
            list.offer(nval);
        }
    }

    public synchronized List<T> getValues() {
        return Collections.unmodifiableList(list);
    }

    public synchronized int size() {
        return list.size();
    }
}
