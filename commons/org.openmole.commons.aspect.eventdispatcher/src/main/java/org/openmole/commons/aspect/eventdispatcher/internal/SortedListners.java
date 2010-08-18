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

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class SortedListners<T> implements Iterable<T> {

    static class ListnerContainer<T2> {
        final T2 listner;
        final Integer priority;

        public ListnerContainer(T2 listner, Integer priority) {
            this.listner = listner;
            this.priority = priority;
        }
    }
    
    static class PriorityComparator<T2> implements Comparator<ListnerContainer<T2>> {

        @Override
        public int compare(ListnerContainer<T2> o1, ListnerContainer<T2> o2) {
            return o2.priority - o1.priority;
        }
        
    }
    
    final Set<ListnerContainer<T>> listners = new TreeSet<ListnerContainer<T>>(new PriorityComparator<T>());
 
    public void registerListner(Integer priority, T listner) {
        listners.add(new ListnerContainer<T>(listner, priority));
    }

    public void registerAllListners(Iterable<? extends Tuple2<Integer, ? extends T>> listeners) {
        for(Tuple2<Integer, ? extends T> listener : listeners) {
            registerListner(listener._1(), listener._2());
        }
    }
    
    public int size() {
        return listners.size();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            Iterator<ListnerContainer<T>> iterator = listners.iterator();
            
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next().listner;
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }
}
