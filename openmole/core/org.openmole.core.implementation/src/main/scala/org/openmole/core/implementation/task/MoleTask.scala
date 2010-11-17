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

package org.openmole.core.implementation.task

import org.openmole.commons.aspect.eventdispatcher.IObjectListenerWithArgs
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.MultipleException
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.service.Priority
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.SynchronizedContext
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.model.execution.IProgress
import scala.collection.mutable.ListBuffer

class MoleTask(name: String, val mole: IMole) extends Task(name) with IMoleTask {

  class ExceptionLister extends IObjectListenerWithArgs[IMoleExecution] {

    val throwables = new ListBuffer[Throwable] 

    override def eventOccured(t: IMoleExecution, os: Array[Object]) = synchronized {
      val moleJob = os(0).asInstanceOf[IMoleJob]

      if (moleJob.state == State.FAILED) {
        moleJob.context.value(GenericTask.Exception.prototype) match {
          case None => throw new InternalProcessingError("BUG: Job has failed but no exception can be found")
          case Some(exception) => throwables += exception
        }
      }
    }

  }


  override protected def process(global: IContext, context: IContext, progress: IProgress) = {

    val globalContext = new SynchronizedContext
    val firstTaskContext = new Context

    for (input <- inputs) {
      if (!input.mode.isOptional || (input.mode.isOptional && context.contains(input.prototype))) {
        context.variable(input.prototype) match {
          case None => throw new InternalProcessingError("Bug:?variable not found.")
          case Some(variable) => firstTaskContext += variable
        }
      }
    }

    val execution = new MoleExecution(mole)

    val exceptionLister = new ExceptionLister
    Activator.getEventDispatcher.registerForObjectChangedSynchronous(execution, Priority.NORMAL, exceptionLister, IMoleExecution.OneJobFinished);

    execution.start(globalContext, firstTaskContext)
    execution.waitUntilEnded

    for (output <- userOutputs) {
      globalContext.variable(output.prototype) match {
        case None =>
        case Some(variable) => context += variable
      }
    }

    val exceptions = exceptionLister.throwables

    if (!exceptions.isEmpty) {
      context += (GenericTask.Exception.prototype, new MultipleException(exceptions))
    }
  }


  override def inputs: IDataSet = {
    val firstTask = mole.root.task match {
      case None => throw new UserBadDataError("First task has not been assigned in the mole of the mole task " + name)
      case Some(t) => t
    }
    new DataSet(super.inputs ++ firstTask.inputs)
  }
}
