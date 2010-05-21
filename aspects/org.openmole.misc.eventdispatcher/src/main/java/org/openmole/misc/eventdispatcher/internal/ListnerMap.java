/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.misc.eventdispatcher.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.misc.tools.structure.Duo;

/**
 *
 * @author reuillon
 */
public class ListnerMap<K, T> {

    final Map<K, List<Duo<Integer,T>>> listnerMap = new WeakHashMap<K, List<Duo<Integer,T>>>();

    List<Duo<Integer,T>> getOrCreateListners(K object) {
        synchronized (listnerMap) {
            List<Duo<Integer,T>> listners = listnerMap.get(object);
            if (listners == null) {
                listners = new LinkedList<Duo<Integer,T>>();
                listnerMap.put(object, listners);
            }
            return listners;
        }
    }

    Iterable<Duo<Integer,T>> getListners(K object) {
        Iterable<Duo<Integer,T>> ret;

        synchronized (listnerMap) {
            ret = listnerMap.get(object);
        }

        if (ret == null) {
            return Collections.EMPTY_LIST;
        } else {
            return ret;
        }
    }

    void registerListner(K object, Integer priority, T listner) {
        List<Duo<Integer,T>> listners = getOrCreateListners(object);

        synchronized (listners) {
            listners.add(new Duo<Integer, T>(priority, listner));
        }
    }
}
