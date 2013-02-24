/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.mole

import org.openmole.core.model.data.Context
import org.openmole.core.model.data.Variable
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ITransition
import org.openmole.misc.eventdispatcher.Event
import scala.collection.mutable.Buffer
import java.util.concurrent.locks.Lock

object ISubMoleExecution {
  case class Finished(val ticket: ITicket) extends Event[ISubMoleExecution]
}

trait ISubMoleExecution {

  def parent: Option[ISubMoleExecution]
  def childs: Iterable[ISubMoleExecution]

  def root: Boolean
  def moleExecution: IMoleExecution

  def jobs: Iterable[IMoleJob]

  def cancel
  def canceled: Boolean

  def masterCapsuleRegistry: IRegistryWithTicket[IMasterCapsule, Context]
  def aggregationTransitionRegistry: IRegistryWithTicket[IAggregationTransition, Buffer[Variable[_]]]
  def transitionRegistry: IRegistryWithTicket[ITransition, Buffer[Variable[_]]]
  def transitionLock: Lock

  def submit(capsule: ICapsule, context: Context, ticket: ITicket)
  def newChild: ISubMoleExecution

}
