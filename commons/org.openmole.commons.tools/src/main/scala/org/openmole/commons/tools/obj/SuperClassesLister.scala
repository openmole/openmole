/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.commons.tools.obj

import scala.collection.mutable.ListBuffer

object SuperClassesLister {
  def listSuperClasses(c: Class[_]): Iterable[Class[_]] = {

    val toExplore = new ListBuffer[Class[_]]
    toExplore += c

    val ret = new ListBuffer[Class[_]]
                                        
    while(!toExplore.isEmpty) {

      val current = toExplore.remove(0)
      ret += current
      val superClass = current.getSuperclass
      if(superClass != null) toExplore += superClass
      for(inter <- current.getInterfaces) toExplore += inter
    }

    ret
  }


  def listImplementedInterfaces(c: Class[_]): Iterable[Class[_]] = {
    val toExplore = new ListBuffer[Class[_]]
    toExplore += c

    val ret = new ListBuffer[Class[_]]

    while(!toExplore.isEmpty) {
      val current = toExplore.remove(0)

      val superClass = current.getSuperclass
      if(superClass != null) toExplore += superClass

      for(inter <- current.getInterfaces) {
        toExplore += inter
        ret += inter
      }
    }

    ret
  }
}
