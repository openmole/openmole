/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author reuillon
 */
public class SortedListnerMap<K, T> {
    final Map<K,SortedListners<T>> listnerMap = new WeakHashMap<K,SortedListners<T>>();

    SortedListners<T> getOrCreateListners(K object) {
        synchronized (listnerMap) {
            SortedListners<T> listners = listnerMap.get(object);
            if (listners == null) {
                listners = new SortedListners<T>();
                listnerMap.put(object, listners);
            }
            return listners;
        }
    }

    public boolean containsListener(K key, T listner) {
        SortedListners<T> sortedListners = listnerMap.get(key);
        if(sortedListners == null) return false;
        return sortedListners.contains(listner);
    }

    Iterable<T> getListners(K object) {
        Iterable<T> ret;

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
        SortedListners<T> listners = getOrCreateListners(object);

        synchronized (listners) {
            listners.registerListner(priority, listner);
        }
    }
}
