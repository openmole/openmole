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

package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class SortedListners<T> implements Iterable<T> {

    final SortedMap<Integer, List<T>> listnersList = new TreeMap<Integer, List<T>>(Collections.reverseOrder());
    final Set<T> listners = new HashSet<T>();

    public void registerListner(Integer priority, T listner) {
        synchronized (listnersList) {
            List<T> listnerForPrio = listnersList.get(priority);
            if (listnerForPrio == null) {
                listnerForPrio = new LinkedList<T>();
                listnersList.put(priority, listnerForPrio);
            }
            listnerForPrio.add(listner);
            listners.add(listner);
        }
    }

    public void registerAllListners(Iterable<? extends Tuple2<Integer, ? extends T>> listeners) {
        for(Tuple2<Integer, ? extends T> listener : listeners) {
            registerListner(listener._1(), listener._2());
        }
    }

    public boolean contains(T o) {
        return listners.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator() {

            Iterator<List<T>> listIterator = listnersList.values().iterator();
            Iterator<T> elementIterator;

            @Override
            public boolean hasNext() {
                if (elementIterator == null || !elementIterator.hasNext()) {
                    if (listIterator.hasNext()) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }

            @Override
            public Object next() {
                if (elementIterator == null || !elementIterator.hasNext()) {
                    elementIterator = listIterator.next().iterator();
                }
                return elementIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
}
