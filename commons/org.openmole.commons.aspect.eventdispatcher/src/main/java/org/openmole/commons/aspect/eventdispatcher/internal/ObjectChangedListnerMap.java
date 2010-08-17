/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedListener;

/**
 *
 * @author reuillon
 */
public class ObjectChangedListnerMap<T extends IObjectChangedListener> {

    final Map<Object, Map<String, SortedListners<T>>> listnerTypeMap = new WeakHashMap<Object, Map<String, SortedListners<T>>>();

    private SortedListners<T> getOrCreateListners(Object object, String type) {
        Map<String, SortedListners<T>> listnersByTypes;

        synchronized (listnerTypeMap) {
            listnersByTypes = listnerTypeMap.get(object);
            if (listnersByTypes == null) {
                listnersByTypes = new TreeMap<String, SortedListners<T>>();
                listnerTypeMap.put(object, listnersByTypes);
            }
        }

        SortedListners<T> listners;

        synchronized (listnersByTypes) {
            listners = listnersByTypes.get(type);
            if (listners == null) {
                listners = new SortedListners<T>();
                listnersByTypes.put(type, listners);
            }
        }

        return listners;
    }

    Iterable<T> getListners(Object object, String type) {
        Map<String, SortedListners<T>> listnersByType;

        synchronized (listnerTypeMap) {
            listnersByType = listnerTypeMap.get(object);
        }

        if (listnersByType == null) {
            return Collections.EMPTY_LIST;
        }

        Iterable<T> ret;

        synchronized (listnersByType) {
            ret = listnersByType.get(type);
        }

        if (ret == null) {
            return Collections.EMPTY_LIST;
        } else {
            return ret;
        }
    }

    void registerListner(Object object, Integer priority, T listner, String type) {
        SortedListners<T> listners = getOrCreateListners(object, type);

        synchronized (listners) {
            listners.registerListner(priority, listner);
        }
    }
}
