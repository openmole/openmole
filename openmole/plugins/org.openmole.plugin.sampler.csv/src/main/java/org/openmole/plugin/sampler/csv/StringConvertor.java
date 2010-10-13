/*
 *  Copyright (C) 2010 leclaire
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
package org.openmole.plugin.sampler.csv;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.openmole.commons.tools.object.Instanciator;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * Class giving a mapping between a string value to be converted and a the target
 * type of the convertion.
 */
public class StringConvertor {
    private static StringConvertor instance = null;
    private static Map<Class,Class> typeMapping = new HashMap<Class,Class>();

 static {
        register(BigInteger.class,BigIntegerMapping.class);
        register(BigDecimal.class,BigDecimalMapping.class);
        register(String.class,StringMapping.class);
        register(File.class,FileMapping.class);
        register(Double.class,DoubleMapping.class);
        register(Integer.class,IntegerMapping.class);
    }

    private static <T> void register(Class<T> cl,
                                     Class mapping){
        typeMapping.put(cl,mapping);
    }
   
    public IStringMapping getConvertor(Class type,
                                       Object... args) throws IllegalArgumentException,
                                                               NoSuchMethodException,
                                                               InstantiationException,
                                                               IllegalAccessException,
                                                               InvocationTargetException{
        if(args.length == 0) return (IStringMapping) Instanciator.instanciate(typeMapping.get(type));
        return (IStringMapping) Instanciator.instanciate(typeMapping.get(type), args);
    }

     public static StringConvertor getInstance() {
        if (instance == null) {
            instance = new StringConvertor();
        }
        return instance;
    }
}
