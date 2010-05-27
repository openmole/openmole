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

package org.openmole.commons.tools.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections15.SortedBag;
import org.apache.commons.collections15.bag.TreeBag;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.structure.Priority;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class HierarchicalRegistry<T> {

    Map<Class, Duo<T, Integer>> registry = new HashMap<Class, Duo<T, Integer>>();

    public void register(Class c, T t) {
        register(c, t, Priority.NORMAL.getValue());
    }

    public void register(Class c, T t, Integer priority) {
       Duo<T, Integer> toRegister = new Duo<T, Integer>(t, priority);
       registry.put(c, toRegister);
    }

    public Set<Class> getAllRegistred(){
        return registry.keySet();
    }

    public Collection<T> getClosestRegistred(Class c) {

        class PrioritySort<T> implements Comparable<PrioritySort>{
            Integer priority;
            T t;

            public PrioritySort(Integer priority, T t) {
                this.priority = priority;
                this.t = t;
            }

            @Override
            public int compareTo(PrioritySort o) {
                return priority - o.priority;
            }
        }

        Queue<Duo<Class, Integer>> toProceed = new LinkedList<Duo<Class, Integer>>();
        toProceed.offer(new Duo<Class, Integer>(c, 0));


        SortedBag<PrioritySort<T>> result = new TreeBag<PrioritySort<T>>();


        while(result.isEmpty() && !toProceed.isEmpty()) {
            Duo<Class, Integer> cur = toProceed.poll();
            Class curClass = cur.getLeft();
            Integer curLevel = cur.getRight();
            
            Duo<T, Integer> registred = registry.get(curClass);

            if(registred != null) {
                result.add(new PrioritySort<T>(registred.getRight(), registred.getLeft()));
                Set<Class> seen = new HashSet<Class>();
                seen.add(curClass);


                while(!toProceed.isEmpty()) {
                    cur = toProceed.poll();

                    if(cur.getRight() == curLevel && !seen.contains(cur.getLeft())) {
                       registred = registry.get(cur.getLeft());
                       if(registred != null) {
                           result.add(new PrioritySort(registred.getRight(), registred.getLeft()));
                       }
                       seen.add(cur.getLeft());
                    } else {
                        toProceed.clear();
                    }
                }
            } else {
                if(curClass != Object.class) {
                    if(!curClass.isInterface()) {
                        Class sc = curClass.getSuperclass();
                        toProceed.offer(new Duo<Class, Integer>(sc, curLevel+1));
                    }
                    for(Class i : curClass.getInterfaces()) {
                        toProceed.offer(new Duo<Class, Integer>(i, curLevel+1));
                    }
                }
            }
        }


        Collection<T> res = new ArrayList<T>(result.size());

        for(PrioritySort<T> p : result) {
            res.add(p.t);
        }

        return res;
    }

}
