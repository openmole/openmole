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
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.model.dataproxy._

class SerializedProxys(val task: Set[ITaskDataProxyUI],
                       val prototype: Set[IPrototypeDataProxyUI],
                       val sampling: Set[ISamplingDataProxyUI],
                       val environment: Set[IEnvironmentDataProxyUI],
                       val incr: Int) {
  def this() = this(Proxys.tasks.toSet,
              Proxys.prototypes.toSet,
              Proxys.samplings.toSet,
              Proxys.environments.toSet,
              0)
    
  def loadProxys = {
    task.foreach{t=>Proxys.tasks += t; ConceptMenu.taskMenu.contents += ConceptMenu.addItem(t)}
    prototype.foreach{p=>Proxys.prototypes+= p; ConceptMenu.prototypeMenu.contents += ConceptMenu.addItem(p)}
    sampling.foreach{s=>Proxys.samplings += s; ConceptMenu.samplingMenu.contents += ConceptMenu.addItem(s)}
    environment.foreach{e=>Proxys.environments+= e; ConceptMenu.environmentMenu.contents += ConceptMenu.addItem(e)}
    Proxys.incr.set(incr)
  }
  
}
