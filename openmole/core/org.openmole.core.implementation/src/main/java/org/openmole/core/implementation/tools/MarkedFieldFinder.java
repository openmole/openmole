/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.implementation.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public class MarkedFieldFinder {


     public static <T> void addAllMarkedFields(Object obj, Class<? extends Annotation> annotation, Collection<T> ret) throws InternalProcessingError {

        LinkedList<Class<?>> toProcess = new LinkedList<Class<?>>();
        Set<Class<?>> processed = new HashSet<Class<?>>();

        Class<?> c = obj.getClass();

        processed.add(c);
        toProcess.push(c);

        while (!toProcess.isEmpty()) {
            Class<?> cur = toProcess.pop();
            for (Field f : cur.getDeclaredFields()) {
                if (f.isAnnotationPresent(annotation)) {
                    Boolean access = f.isAccessible();
                    f.setAccessible(true);
                    Object target;
                    try {
                        target = f.get(obj);
                    } catch (IllegalArgumentException e) {
                        throw new InternalProcessingError(e);
                    } catch (IllegalAccessException e) {
                        throw new InternalProcessingError(e);
                    }

                    if(target == null) {
                        throw new NullPointerException("Target field " + f.toString() + " is null.");
                    }

                    try {
                        ret.add((T) target);
                    } catch (ClassCastException e) {
                        throw new InternalProcessingError(e, "Annotation " + annotation.getCanonicalName() + " present on wrong type; actual type is :" + MarkedFieldFinder.class.getCanonicalName() + " for field " + f.getName());
                    }
                    f.setAccessible(access);
                }
            }
            if (cur.getSuperclass() != null) {
                processed.add(cur.getSuperclass());
                toProcess.push(cur.getSuperclass());
            }
            for (Class<?> inter : cur.getInterfaces()) {
                if (!processed.contains(inter)) {
                    processed.add(inter);
                    toProcess.add(inter);
                }
            }
        }
    }

}
