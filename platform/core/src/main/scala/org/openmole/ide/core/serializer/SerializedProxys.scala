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

package org.openmole.ide.core.serializer

import org.openmole.ide.core.palette._
import scala.collection.mutable.HashSet

class SerializedProxys(var dataTaskProxys: HashSet[TaskDataProxyUI],
                       var dataPrototypeProxys: HashSet[PrototypeDataProxyUI],
                       var dataSamplingProxys: HashSet[SamplingDataProxyUI],
                       var dataEnvironmentProxys: HashSet[EnvironmentDataProxyUI]) {
  
  def loadProxys = {
    ElementFactories.dataTaskProxys = dataTaskProxys
    ElementFactories.dataPrototypeProxys = dataPrototypeProxys
    ElementFactories.dataSamplingProxys = dataSamplingProxys
    ElementFactories.dataEnvironmentProxys = dataEnvironmentProxys
  }
  
}
