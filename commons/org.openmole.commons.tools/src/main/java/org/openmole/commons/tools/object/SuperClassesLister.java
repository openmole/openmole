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
package org.openmole.commons.tools.object;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 *
 * @author reuillon
 */
public class SuperClassesLister {
    public static List<Class> listSuperClasses(Class c) {

        LinkedList<Class> toExplore = new LinkedList<Class>();
        toExplore.push(c);

        List<Class> ret = new LinkedList<Class>();

        while(!toExplore.isEmpty()) {

            Class current = toExplore.pop();
            ret.add(current);

            Class superClass = current.getSuperclass();
            if(superClass != null)
                toExplore.add(superClass);

            for(Class inter: current.getInterfaces()) {
                toExplore.add(inter);
            }
        }

        return ret;
    }


    public static List<Class> listImplementedInterfaces(Class c) {

        Stack<Class> toExplore = new Stack<Class>();
        toExplore.push(c);

        List<Class> ret = new LinkedList<Class>();

        while(!toExplore.isEmpty()) {

            Class current = toExplore.pop();

            Class superClass = current.getSuperclass();
            if(superClass != null)
                toExplore.add(superClass);

            for(Class inter: current.getInterfaces()) {
                toExplore.add(inter);
                ret.add(inter);
            }
        }

        return ret;
    }
}
