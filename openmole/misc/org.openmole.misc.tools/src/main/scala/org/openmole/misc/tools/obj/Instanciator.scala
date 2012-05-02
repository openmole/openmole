/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.tools.obj

import org.objenesis.ObjenesisStd
import scala.collection.mutable.ListBuffer

object Instanciator {
  private val objenesis = new ObjenesisStd

  def instanciate[T](c: Class[T]): T = objenesis.newInstance(c).asInstanceOf[T]

  def instanciate[T](c: Class[T], first: Object, args: Array[Object]): T = instanciate(c, (ListBuffer(first) ++ args): _*)

  def instanciate[T](c: Class[T], args: Object*): T = {
    //val argsTypes = args.map{_.getClass}.toArray
    val ctr = c.getConstructor(args.map { _.getClass }.toArray[Class[_]]: _*)

    val isAccessible = ctr.isAccessible
    ctr.setAccessible(true)
    val instance = ctr.newInstance(args)
    ctr.setAccessible(isAccessible)

    instance
  }
}
