/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.List;
import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedListener;
import org.openmole.commons.tools.object.SuperClassesLister;
import org.openmole.commons.tools.structure.Duo;


/**
 *
 * @author reuillon
 */
public class ObjectConstructedListnerMap<T extends IObjectConstructedListener> {
    ListnerMap<Class, T> listnerMap = new ListnerMap<Class, T>();

    void registerListner(Class object, Integer priority, T listner) {
        listnerMap.registerListner(object, priority, listner);
    }

    List<Duo<Integer,T>> getOrCreateListners(Class object) {
        return listnerMap.getOrCreateListners(object);
    }

    Iterable<T> getListners(Class c) {
        SortedListners<T> ret = new SortedListners<T>();

        List<Class> classes = SuperClassesLister.listImplementedInterfaces(c);
        classes.add(c);

        for(Class cl : classes) {
            ret.registerAllListners(listnerMap.getListners(cl));
        }

        return ret;
    }



}
