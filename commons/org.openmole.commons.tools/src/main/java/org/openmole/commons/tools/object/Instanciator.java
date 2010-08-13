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

package org.openmole.commons.tools.object;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * TODO Description <code>Instanciator</code>
 */

public class Instanciator {
    
    private static Objenesis objenesis = new ObjenesisStd();
    
    /**
     * TODO Description of <code>instanciate</code>
     *
     * @param <T>
     * @param type
     * @return
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static <T> T instanciate(Class<T> type) throws IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    	return (T) objenesis.newInstance(type);
    }

  

       public static <T> T instanciate(Class<T> type, Object... args) throws IllegalArgumentException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
          Collection<Class> argsTypes = new ArrayList<Class>(args.length);

          for(Object arg: args) {
              argsTypes.add(arg.getClass());
          }

          Constructor<T> ctr = type.getConstructor(argsTypes.toArray(new Class[argsTypes.size()]));

          boolean isAccessible = ctr.isAccessible();
          ctr.setAccessible(true);
          T instance = ctr.newInstance(args);
          ctr.setAccessible(isAccessible);

          return instance;
       }

}

