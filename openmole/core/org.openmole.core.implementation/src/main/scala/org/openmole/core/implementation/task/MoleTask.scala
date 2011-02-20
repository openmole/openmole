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

import java.util.logging.Logger
import org.openmole.commons.aspect.eventdispatcher.EventDispatcher
import org.openmole.commons.aspect.eventdispatcher.IObjectListenerWithArgs
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.MultipleException
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.service.Priority
import org.openmole.commons.tools.obj.ClassUtils._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.mole.MoleJobRegistry
import org.openmole.core.implementation.tools.ContextAggregator._
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

class MoleTask(name: String, val mole: IMole) extends Task(name) with IMoleTask {

  //def this(name: String, mole: IMole) = this(name, mole, false)
  var forceArray = List.empty[IPrototype[_]]
  
  class ResultGathering extends IObjectListenerWithArgs[IMoleExecution] {

    val throwables = new ListBuffer[Throwable] 
    val variables = new ListBuffer[IVariable[_]] 
    
    override def eventOccured(t: IMoleExecution, os: Array[Object]) = synchronized {
      val moleJob = os(0).asInstanceOf[IMoleJob]

      //Logger.getLogger(classOf[MoleTask].getName).fine(moleJob.task.name + " " + moleJob.state.toString)

      moleJob.state match {
        case State.FAILED =>
          throwables += moleJob.context.value(GenericTask.Exception.prototype).getOrElse(new InternalProcessingError("BUG: Job has failed but no exception can be found"))
        case State.COMPLETED =>
          val e = MoleJobRegistry(moleJob).getOrElse(throw new InternalProcessingError("Capsule for " + moleJob.task.name + " not found in registry."))
          outputCapsules.get(e._2) match {
            case None => //Logger.getLogger(classOf[MoleTask].getName).fine("No output registred for " + moleJob.task.name)
            case Some(prototypes) => 
              val ctx = new Context
              for(p <- prototypes) {
                //Logger.getLogger(classOf[MoleTask].getName).fine(e._2.toString + " " + p.toString)
                moleJob.context.variable(p).foreach{v: IVariable[_] => ctx += v}
              }
              variables ++= ctx
          }
          
        case _ =>
      }
    }
  }
   

  private val outputCapsules = new HashMap[IGenericCapsule, ListBuffer[String]]

  override protected def process(context: IContext, progress: IProgress) = {
    val firstTaskContext = new Context

    for (input <- inputs) {
      if (!input.mode.isOptional || (input.mode.isOptional && context.contains(input.prototype))) {
        firstTaskContext += context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found."))        
      }
    }

    val execution = new MoleExecution(mole)
    val resultGathering = new ResultGathering
    
    EventDispatcher.registerForObjectChangedSynchronous(execution, Priority.NORMAL, resultGathering, IMoleExecution.OneJobStatusChanged)

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    val toArrayMap = TreeMap.empty[String, Manifest[_]] ++ forceArray.map( e => e.name -> manifest(e.`type`))
    
    aggregate(userOutputs, toArrayMap, resultGathering.variables).foreach {
      context += _
    }

    val exceptions = resultGathering.throwables

    if (!exceptions.isEmpty) {
      context += (GenericTask.Exception.prototype, new MultipleException(exceptions))
    }
  }

  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_]): Unit = addOutput(capsule, new Data(prototype))

  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_],masks: Array[DataModeMask]): Unit = addOutput(capsule, new Data(prototype, masks))
 
  def addOutput(capsule: IGenericCapsule, data: IData[_]): Unit = {
    addOutput(data)
    outputCapsules.getOrElseUpdate(capsule, new ListBuffer[String]) += data.prototype.name
  }
  
  override def inputs: IDataSet = {
    val firstTask = mole.root.task.getOrElse(throw new UserBadDataError("First task has not been assigned in the mole of the mole task " + name))
    new DataSet(super.inputs ++ firstTask.inputs)
  }
  
  override def forceArray(prototype: IPrototype[_]): this.type = {
    forceArray = prototype +: forceArray
    this
  }
}
