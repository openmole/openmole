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
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.sampling.csv;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.openmole.misc.tools.obj.Instanciator;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 *
 * Class giving a mapping between a string value to be converted and a the target
 * type of the convertion.
 */
public class StringConvertor {

    private static StringConvertor instance = null;
    private static Map<Class, IStringMapping> typeMapping = new HashMap<Class, IStringMapping>();

    static {
        register(BigInteger.class, new BigIntegerMapping());
        register(BigDecimal.class, new BigDecimalMapping());
        register(String.class, new StringMapping());
        register(Double.class, new DoubleMapping());
        register(Integer.class, new IntegerMapping());
    }

    public static <T> void register(Class<T> cl, IStringMapping mapping) {
        typeMapping.put(cl, mapping);
    }

    public static IStringMapping getConvertor(Class type) {
        return typeMapping.get(type);
    }
}
