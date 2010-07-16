/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import scala.Tuple2;

/**
 *
 * @author reuillon
 */
public class ListnerMap<K, T> {

    final Map<K, List<Tuple2<Integer,T>>> listnerMap = new WeakHashMap<K, List<Tuple2<Integer,T>>>();

    List<Tuple2<Integer,T>> getOrCreateListners(K object) {
        synchronized (listnerMap) {
            List<Tuple2<Integer,T>> listners = listnerMap.get(object);
            if (listners == null) {
                listners = new LinkedList<Tuple2<Integer,T>>();
                listnerMap.put(object, listners);
            }
            return listners;
        }
    }

    Iterable<Tuple2<Integer,T>> getListners(K object) {
        Iterable<Tuple2<Integer,T>> ret;

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
        List<Tuple2<Integer,T>> listners = getOrCreateListners(object);

        synchronized (listners) {
            listners.add(new Tuple2<Integer, T>(priority, listner));
        }
    }
}
