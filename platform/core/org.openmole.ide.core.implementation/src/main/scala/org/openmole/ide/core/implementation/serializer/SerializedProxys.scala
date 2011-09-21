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

class SerializedProxys(val task: Iterable[(Int,ITaskDataProxyUI)],
                       val prototype: Iterable[(Int,IPrototypeDataProxyUI)],
                       val sampling: Iterable[(Int,ISamplingDataProxyUI)],
                       val environment: Iterable[(Int,IEnvironmentDataProxyUI)],
                       val domain: Iterable[(Int,IDomainDataProxyUI)],
                       val incr: Int) {
  
  def loadProxys = {
    task.foreach(t=>Proxys.addTaskElement(t._2,t._1))
    prototype.foreach(p=>Proxys.addPrototypeElement(p._2,p._1))
    sampling.foreach(s=>Proxys.addSamplingElement(s._2,s._1))
    environment.foreach(e=>Proxys.addEnvironmentElement(e._2,e._1))
    domain.foreach(d=>Proxys.addDomainElement(d._2,d._1))
    Proxys.incr.set(incr)
  }
  
}
