/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.mole

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.mole.ILocalCommunication
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IGenericTransition
import scala.collection.mutable.ListBuffer

class LocalCommunication extends ILocalCommunication {
  val transitionRegistry = new RegistryWithTicket[IGenericTransition, IContextBuffer]
  
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, IContextBuffer]
 
  val dataChannelRegistry = new RegistryWithTicket[IDataChannel, IContextBuffer]
}
