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

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.tools.IRegistryWithTicket
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IGenericTransition

object ISubMoleExecution {
  val Finished = "Finished"
  //val AllJobsWaitingInGroup = "AllJobsWaitingInGroup"
}

trait ISubMoleExecution {
 
  def parent: Option[ISubMoleExecution]
  def isRoot: Boolean
  def moleExecution: IMoleExecution

  def nbJobInProgess: Int
    
  def addChild(submoleExecution: ISubMoleExecution)
  def removeChild(submoleExecution: ISubMoleExecution)
   
  def incNbJobInProgress(value: Int) 
  def decNbJobInProgress(value: Int) 
    
  def incNbJobWaitingInGroup(value: Int)
  def decNbJobWaitingInGroup(value: Int)
  
  def cancel
  
  def aggregationTransitionRegistry: IRegistryWithTicket[IAggregationTransition, IContextBuffer]
  def transitionRegistry: IRegistryWithTicket[IGenericTransition, IContextBuffer]

  def submit(capsule: IGenericCapsule, context: IContext, ticket: ITicket)
  def group(moleJob: IMoleJob, capsule: IGenericCapsule, grouping: Option[IGroupingStrategy])

}
