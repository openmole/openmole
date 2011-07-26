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

import org.openmole.core.model.task.ITask
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.MultipleException
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Priority
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.implementation.data.Context._
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.mole.MoleJobRegistry
import org.openmole.core.implementation.tools.ContextAggregator._
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.model.data.IPrototype
import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

class MoleTask(name: String, val mole: IMole) extends Task(name) with IMoleTask {

  var forcedArray = List.empty[IPrototype[_]]
  
  class ResultGathering extends IObjectListenerWithArgs[IMoleExecution] {

    val variables = new ListBuffer[IVariable[_]] 
    
    override def eventOccured(t: IMoleExecution, os: Array[Object]) = synchronized {
      val moleJob = os(0).asInstanceOf[IMoleJob]
      val capsule = os(1).asInstanceOf[ICapsule]

      outputCapsules.get(capsule) match {
        case None => //Logger.getLogger(classOf[MoleTask].getName).fine("No output registred for " + moleJob.task.name)
        case Some(prototypes) =>
          variables ++= prototypes.flatMap(p => moleJob.context.variable(p).toList)
      }
    }
  }
   

  private val outputCapsules = new HashMap[ICapsule, ListBuffer[String]]

  override protected def process(context: IContext) = {
    val firstTaskContext = inputs.foldLeft(List.empty[IVariable[_]]) {
      (acc, input) =>
      if (!(input.mode is optional) || ((input.mode is optional) && context.contains(input.prototype)))
        context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found.")) :: acc
      else acc
    }.toContext

    val execution = new MoleExecution(mole)
    val resultGathering = new ResultGathering
    
    EventDispatcher.registerForObjectChangedSynchronous(execution, Priority.NORMAL, resultGathering, IMoleExecution.JobInCapsuleFinished)
    
    execution.start(firstTaskContext)
    execution.waitUntilEnded

    val toArrayMap = TreeMap.empty[String, Manifest[_]] ++ forcedArray.map(e => e.name -> e.`type`)
    
    context ++ aggregate(userOutputs, toArrayMap, resultGathering.variables)
  }

  def addOutput(capsule: ICapsule, prototype: IPrototype[_], forceArray: Boolean): this.type = addOutput(capsule, new Data(prototype), forceArray)

  def addOutput(capsule: ICapsule, prototype: IPrototype[_],masks: Array[DataModeMask], forceArray: Boolean): this.type = addOutput(capsule, new Data(prototype, masks), forceArray)
 
  def addOutput(capsule: ICapsule, data: IData[_], forceArray: Boolean): this.type = {
    addOutput(data)
    outputCapsules.getOrElseUpdate(capsule, new ListBuffer[String]) += data.prototype.name
    if(forceArray) this.forceArray(data.prototype)
    this
  }
 
  def addOutput(capsule: ICapsule, prototype: IPrototype[_]): this.type = addOutput(capsule, prototype, false)

  def addOutput(capsule: ICapsule, data: IData[_]): this.type = addOutput(capsule, data, false)
  
  def addOutput(capsule: ICapsule, prototype: IPrototype[_],masks: Array[DataModeMask]): this.type = addOutput(capsule, prototype, masks, false)
 
  override def inputs: IDataSet = {
    val firstTask = mole.root.task.getOrElse(throw new UserBadDataError("First task has not been assigned in the mole of the mole task " + name))
    new DataSet(super.inputs ++ firstTask.inputs)
  }
  
  private def forceArray(prototype: IPrototype[_]) = forcedArray = prototype +: forcedArray
}
