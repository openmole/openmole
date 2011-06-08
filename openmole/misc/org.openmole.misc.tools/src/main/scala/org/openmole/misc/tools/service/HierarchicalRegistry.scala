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

package org.openmole.misc.tools.service

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class HierarchicalRegistry[T] {

  val registry = new HashMap[Class[_], (T, Int)]

  def register(c: Class[_], t: T): Unit = register(c, t, Priority.NORMAL)

  def register(c: Class[_], t: T, priority: Int): Unit = registry += c -> (t,priority)

  def allRegistred: Set[Class[_]] = registry.keySet.toSet

  def closestRegistred(c: Class[_]): Iterable[T] = {
    val toProceed = new ListBuffer[(Class[_], Int)]
    toProceed += ((c, 0))

    val result = new ListBuffer[(T, Int)]()

    while(result.isEmpty && !toProceed.isEmpty) {
      val cur = toProceed.remove(0)
      registry.get(cur._1) match {

        case Some(registred) => {
            result += registred
            val seen = new HashSet[Class[_]]
            seen += cur._1

            while(!toProceed.isEmpty) {
              val curProc = toProceed.remove(0)

              if(curProc._2 == cur._2 && !seen.contains(curProc._1)) {
                registry.get(curProc._1) match {
                  case None =>
                  case Some(registred) => result += registred
                }
                seen += curProc._1
              } else {
                toProceed.clear
              }
            }
          } 
        case None => {
            if(cur._1 != classOf[Object]) {
              if(!cur._1.isInterface) {
                val sc = cur._1.getSuperclass
                toProceed += ((sc, cur._2+1))
              }
              for(i <- cur._1.getInterfaces) {
                toProceed += ((i, cur._2+1))
              }
            }
          }
      }
    }
    result.sortWith{case(l,r) => l._2 < r._2}.map{case(r,p) => r}
  }

}
