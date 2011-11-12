/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.serializer

import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.dataproxy._

class SerializedProxys(val task: Map[Int,ITaskDataProxyUI],
                       val prototype: Map[Int,IPrototypeDataProxyUI],
                       val sampling: Map[Int,ISamplingDataProxyUI],
                       val environment: Map[Int,IEnvironmentDataProxyUI],
                       val incr: Int) {
  def this() = this(Proxys.tasks.toMap,
              Proxys.prototypes.toMap,
              Proxys.samplings.toMap,
              Proxys.environments.toMap,
              0)
    
  def loadProxys = {
    task.foreach(t=>Proxys.addTaskElement(t._2,t._1))
    prototype.foreach(p=>Proxys.addPrototypeElement(p._2,p._1))
    sampling.foreach(s=>Proxys.addSamplingElement(s._2,s._1))
    environment.foreach(e=>Proxys.addEnvironmentElement(e._2,e._1))
    Proxys.incr.set(incr)
  }
  
}
