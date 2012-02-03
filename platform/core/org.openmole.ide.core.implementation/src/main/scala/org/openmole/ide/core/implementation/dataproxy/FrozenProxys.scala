/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dataproxy

import com.rits.cloning.Cloner
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.implementation.serializer.SerializedProxys
import scala.collection.mutable.WeakHashMap

object FrozenProxys {
  val maps= new WeakHashMap[MoleSceneTopComponent,SerializedProxys]
  def task(tc: MoleSceneTopComponent) = maps(tc).task
  def prototype(tc: MoleSceneTopComponent) = maps(tc).prototype
  def sampling(tc: MoleSceneTopComponent) = maps(tc).sampling  
  def environment(tc: MoleSceneTopComponent) = maps(tc).environment
  def freeze(tc: MoleSceneTopComponent) = {
    val cloner = new Cloner
    maps+= tc-> new SerializedProxys(cloner.deepClone(Proxys.tasks).toSet,
                                     cloner.deepClone(Proxys.prototypes).toSet,
                                     cloner.deepClone(Proxys.sampling).toSet,
                                     cloner.deepClone(Proxys.environment).toSet,
                                     Proxys.incr.get+1)
  }
  def clear = maps.clear
}
