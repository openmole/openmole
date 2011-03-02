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

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Parameter
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.{IContext,IData,IParameter,IVariable,IPrototype,DataModeMask, IDataSet}
import org.openmole.core.model.job.ITimeStamp
import org.openmole.core.model.task.{IGenericTask,IResource}
import org.openmole.core.model.execution.IProgress
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object GenericTask {
  val Timestamps = new Data[ArrayBuffer[ITimeStamp]]("Timestamps#", classOf[ArrayBuffer[ITimeStamp]], Array(SYSTEM))
  val Exception = new Data[Throwable]("Exception#", classOf[Throwable], Array(OPTIONAL, SYSTEM))
}


abstract class GenericTask(val name: String) extends IGenericTask {

  private var _inputs = new TreeMap[String, IData[_]]
  private var _outputs = new TreeMap[String, IData[_]]
  private var _resources = new HashSet[IResource]
  private var _parameters = new TreeMap[String, IParameter[_]]
  
  addOutput(GenericTask.Timestamps)
  addOutput(GenericTask.Exception)
  
  protected def verifyInput(context: IContext) = {
    for (d <- inputs) {
     if (!d.mode.isOptional) {
        val p = d.prototype
        context.variable(p.name) match {
          case None => throw new UserBadDataError("Input data named \"" + p.name + "\" of type \"" + p.`type`.toString + "\" required by the task \"" + name + "\" has not been found");
          case Some(v) => if (!p.isAssignableFrom(v.prototype)) throw new UserBadDataError("Input data named \"" + p.name + "\" required by the task \"" + name + "\" has the wrong type: \"" + v.prototype.`type`.toString + "\" instead of \"" + p.`type`.toString + "\" required")
        }
      }
    }
  }

  protected def filterOutput(context: IContext): Unit = {
    val vars = new ListBuffer[IVariable[_]]
   
    for (d <- outputs) {
      val p = d.prototype
      context.variable(p) match {
        case None => 
          if (!d.mode.isOptional) throw new UserBadDataError("Variable " + p.name + " of type " + p.`type`.toString +" in not optional and has not found in output of task" + name +".")
        case Some(v) =>
          if (p.accepts(v.value)) vars += v
          else throw new UserBadDataError("Output value of variable " + p.name + " (prototype: "+ v.prototype.`type`.toString +") is instance of class '" + v.value.asInstanceOf[AnyRef].getClass + "' and doesn't match the expected class '" + p.`type`.toString + "' in task" + name + ".")
      }
    }

    context.clean
    context ++= vars
  }

  private def init(context: IContext) = {
    for (parameter <- parameters) {
      if (parameter.`override` || !context.containsVariableWithName(parameter.variable.prototype)) {
        context += parameter.variable
      }
    }

    verifyInput(context)
  }

  /**
   * The main operation of the processor.
   * @param context
   * @param progress
   */
  @throws(classOf[Throwable])
  protected def process(context: IContext, progress: IProgress): Unit
   

  /* (non-Javadoc)
   * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
   */
  override def perform(context: IContext, progress: IProgress) = {
    try {
      deploy
      init(context)
      process(context, progress)
      end(context)
    } catch {
      case e => throw new InternalProcessingError(e, "Error in task " + name)
    }
  }

  private def end(context: IContext) = filterOutput(context)

  override def addOutput(prototype: IPrototype[_]): this.type = addOutput(new Data(prototype))

  override def addOutput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type = addOutput(new Data(prototype, masks)); this
 
  override def addOutput(data: IData[_]): this.type =  {
    _outputs += ((data.prototype.name, data))
    this
  }
  
  override def addResource(resource: IResource): this.type = {
    _resources += resource
    this
  }
  
  override def addInput(prototype: IPrototype[_]): this.type = addInput(new Data(prototype))

  override def addInput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type = addInput(new Data(prototype, masks))

  override def addInput(data: IData[_]): this.type = {
    _inputs += ((data.prototype.name,data))
    this
  }

  override def addInput(dataSet: IDataSet): this.type = {
    for(data <- dataSet) addInput(data)     
    this
  }

  override def addOutput(dataSet: IDataSet): this.type = {
    for(data <- dataSet) addOutput(data)
    this
  }

  override def containsInput(name: String): Boolean =  _inputs.contains(name)

  override def containsInput(prototype: IPrototype[_]): Boolean = containsInput(prototype.name)

  override def containsOutput(name: String): Boolean =  _outputs.contains(name)

  override def containsOutput(prototype: IPrototype[_]): Boolean = _outputs.contains(prototype.name)

  override def inputs: IDataSet = new DataSet(_inputs)
    
  override def outputs: IDataSet = new DataSet(_outputs)
 
  @transient override lazy val userInputs: IDataSet =  new DataSet(inputs.filter(!_.mode.isSystem))
   
  @transient override lazy val userOutputs: IDataSet = new DataSet(outputs.filter(!_.mode.isSystem))

  def resources: Iterable[IResource] = _resources
 
  override def deploy = for (resource <- resources) resource.deploy   
    
  override def addParameter(parameter: IParameter[_]): this.type = {
    _parameters += ((parameter.variable.prototype.name,parameter))
    this
  }
    
  override def addParameter[T](prototype: IPrototype[T] , value: T): this.type = addParameter(new Parameter[T](prototype, value))
    
  override def addParameter[T](prototype: IPrototype[T], value: T, `override`: Boolean): this.type = addParameter(new Parameter[T](prototype, value, `override`))
    
  override def parameters: Iterable[IParameter[_]]= _parameters.values
   
  override def toString: String = name       
    
}
