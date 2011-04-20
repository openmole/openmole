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

import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.tools.IContextBuffer
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IGenericTransition
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.tools.RegistryWithTicket
import org.openmole.core.model.job.IJob
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.mole.IMoleExecution
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet

object SubMoleExecution {
  
  def apply(moleExecution: IMoleExecution): SubMoleExecution = {
    val ret = new SubMoleExecution(None, moleExecution)
    moleExecution.register(ret)
    ret
  }
  
  def apply(moleExecution: IMoleExecution, parent: ISubMoleExecution): SubMoleExecution =  {
    val ret = new SubMoleExecution(Some(parent), moleExecution)
    moleExecution.register(ret)
    ret
  }
  
}


class SubMoleExecution(val parent: Option[ISubMoleExecution], val moleExecution: IMoleExecution) extends ISubMoleExecution {

  private var submittedJobs = TreeSet[IMoleJob]()
  private var waiting = List[IJob]()
  private var _nbJobInProgress = 0
  private var _nbJobWaitingInGroup = 0
  private var childs = new HashSet[ISubMoleExecution]
  private var _canceled = false
  
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, IContextBuffer]
  val transitionRegistry = new RegistryWithTicket[IGenericTransition, IContextBuffer]

  parrentApply(p => p.addChild(this))

  override def isRoot = !parent.isDefined
  
  override def nbJobInProgess = _nbJobInProgress //_nbJobInProgress
  
  override def += (moleJob: IMoleJob) = synchronized {
    submittedJobs += moleJob
    incNbJobInProgress(1)
  }
  
  override def -= (moleJob: IMoleJob) = synchronized {
    submittedJobs -= moleJob
    //println("Remove " + moleJob.state + " from " + this)
    decNbJobInProgress(1)
  }
  
  override def addWaiting(job: IJob) = synchronized {
    waiting :+= job
    checkAllJobsWaitingInGroup
  }
  
  override def removeAllWaiting: Iterable[IJob]= synchronized {
    val ret = waiting
    waiting = List.empty[IJob]
    decNbJobWaitingInGroup(ret.map{_.moleJobs.size}.sum)
    ret
  }
  
  override def cancel = synchronized {
    _canceled = true
    submittedJobs.foreach{_.cancel}
    childs.foreach{c => c.cancel}
    parrentApply(p => p.removeChild(this))
  }
  
  override def addChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs += submoleExecution
  }

  override def removeChild(submoleExecution: ISubMoleExecution) = synchronized {
    childs -= submoleExecution
  }

  
  override def incNbJobInProgress(nb: Int) = synchronized {
    _nbJobInProgress += nb
    parrentApply(p => p.incNbJobInProgress(nb))
  }

  override def decNbJobInProgress(nb: Int) = synchronized {
    _nbJobInProgress -= nb
    checkAllJobsWaitingInGroup
    parrentApply(p => p.decNbJobInProgress(nb))
  }
  
  override def incNbJobWaitingInGroup(nb: Int) = synchronized {
     //   println("Inc waiting " + nb + " " + this)
    _nbJobWaitingInGroup += nb
    checkAllJobsWaitingInGroup
    parrentApply(p => p.incNbJobWaitingInGroup(nb))
  }

  override def decNbJobWaitingInGroup(nb: Int) = synchronized {
    _nbJobWaitingInGroup -= nb
    parrentApply(p => p.decNbJobWaitingInGroup(nb))
  }
  
  override def canceled = _canceled
  
  private def parrentApply(f: ISubMoleExecution => Unit) = {
    parent match {
      case None => 
      case Some(p) => f(p)
    }
  }
    
  private def checkAllJobsWaitingInGroup = {
    if(nbJobInProgess == _nbJobWaitingInGroup && _nbJobWaitingInGroup > 0)
      EventDispatcher.objectChanged(this, ISubMoleExecution.AllJobsWaitingInGroup)
  }
  

}
