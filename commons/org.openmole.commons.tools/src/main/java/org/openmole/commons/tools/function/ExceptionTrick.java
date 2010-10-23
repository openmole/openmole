/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.commons.tools.function;

/**
 *
 * @author reuillon
 */
public class ExceptionTrick {
   public static <T extends Throwable> void chucks(Class<T> clazz) throws T {}
  
   @SuppressWarnings("unchecked")
   public static <T extends Throwable, A>  A pervertException(Throwable x) throws T {
      throw (T) x;
   }

}
