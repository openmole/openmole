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

package org.openmole.core.implementation.task

import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask

class MoleTask(val name: String, val mole: IMole, val lastCapsule: ICapsule) extends Task with IMoleTask {

  var implicits: IContext = new Context
  
  def addImplicit[T](p: IPrototype[T], v: T) = implicits = implicits + new Variable(p, v)
  def addImplicit[T](name: String, v: T) = implicits = implicits + new Variable(name, v) 
  
  class ResultGathering extends EventListener[IMoleExecution] {
    var lastContext: Option[IContext] = None
     
    override def triggered(obj: IMoleExecution, ev: Event[IMoleExecution]) = synchronized {
      ev match {
        case ev: IMoleExecution.JobInCapsuleFinished =>  
          if(ev.capsule == lastCapsule) lastContext = Some(ev.moleJob.context)
        case _ =>
      }
    }
  }

  override protected def process(context: IContext) = {
    val firstTaskContext = inputs.foldLeft(List.empty[IVariable[_]]) {
      (acc, input) =>
      if (!(input.mode is optional) || ((input.mode is optional) && context.contains(input.prototype)))
        context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found.")) :: acc
      else acc
    }.toContext

    val execution = new MoleExecution(mole)
    val resultGathering = new ResultGathering

    EventDispatcher.listen(execution: IMoleExecution, resultGathering, classOf[IMoleExecution.JobInCapsuleFinished])

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    context + resultGathering.lastContext.getOrElse(throw new UserBadDataError("Last capsule " + lastCapsule + " has never been executed."))
  }

  override def inputs: IDataSet = new DataSet((super.inputs ++ mole.root.taskOrException.inputs).toList)
  override def outputs: IDataSet = new DataSet((super.inputs ++ lastCapsule.taskOrException.outputs).toList)
 
}
