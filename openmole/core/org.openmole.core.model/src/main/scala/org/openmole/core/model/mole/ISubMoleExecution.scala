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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.mole

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.ITransition
import org.openmole.misc.eventdispatcher.Event
import scala.collection.mutable.Buffer

object ISubMoleExecution {
  case class Finished(val ticket: ITicket) extends Event[ISubMoleExecution]
}

trait ISubMoleExecution {
 
  def parent: Option[ISubMoleExecution]
  def childs: Iterable[ISubMoleExecution]
  
  def isRoot: Boolean
  def moleExecution: IMoleExecution
    
  //def +=(submoleExecution: ISubMoleExecution)
  //def -=(submoleExecution: ISubMoleExecution)
   
  def jobs: Iterable[IMoleJob]
  //def nbJobInProgress: Int
  //def nbJobGrouping: Int
  //def submitting_=(b: Boolean)
  
  def cancel
  
  def masterCapsuleRegistry: IRegistryWithTicket[IMasterCapsule, Iterable[IVariable[_]]]
  def aggregationTransitionRegistry: IRegistryWithTicket[IAggregationTransition, Buffer[IVariable[_]]]
  def transitionRegistry: IRegistryWithTicket[ITransition, Buffer[IVariable[_]]]

  def submit(capsule: ICapsule, context: IContext, ticket: ITicket)
  def group(moleJob: IMoleJob, capsule: ICapsule, grouping: Option[IGroupingStrategy])
  def newChild: ISubMoleExecution 
 
}
