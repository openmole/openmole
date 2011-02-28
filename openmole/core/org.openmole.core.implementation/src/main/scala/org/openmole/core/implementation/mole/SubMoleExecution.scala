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
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.model.mole.IMoleExecution

object SubMoleExecution {
  def apply(moleExecution: IMoleExecution): SubMoleExecution = {
    val ret = new SubMoleExecution(None)
    moleExecution.register(ret)
    ret
  }
  
  def apply(moleExecution: IMoleExecution, parent: ISubMoleExecution): SubMoleExecution =  {
    val ret = new SubMoleExecution(Some(parent))
    moleExecution.register(ret)
    ret
  }
  
}


class SubMoleExecution(val parent: Option[ISubMoleExecution]) extends ISubMoleExecution {

  private var _nbJobInProgress = 0
  private var _nbJobWaitingInGroup = 0


  override def nbJobInProgess = _nbJobInProgress

  override def incNbJobInProgress(nb: Int) = {
    _nbJobInProgress += nb
    parent match {
      case None => 
      case Some(t) => t.incNbJobInProgress(nb)
    }
  }

  override def incNbJobWaitingInGroup(nb: Int) = {
    _nbJobWaitingInGroup += nb
    checkAllJobsWaitingInGroup
  }

  override def decNbJobWaitingInGroup(value: Int) {
    _nbJobWaitingInGroup -= value
  }

  private def checkAllJobsWaitingInGroup =  {
    if(nbJobInProgess == _nbJobWaitingInGroup && _nbJobWaitingInGroup > 0) {
      EventDispatcher.objectChanged(this, ISubMoleExecution.AllJobsWaitingInGroup)
    }
  }

  override def decNbJobInProgress(nb: Int) = {
    _nbJobInProgress -= nb
    checkAllJobsWaitingInGroup
    parent match {
      case None =>
      case Some(t) => t.decNbJobInProgress(nb)
    }
  }

  override def isRoot: Boolean = {
    parent match {
      case None => true
      case Some(t) => false
    }
  }

}
