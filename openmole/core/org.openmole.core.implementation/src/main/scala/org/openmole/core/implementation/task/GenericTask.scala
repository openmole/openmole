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

import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Parameter
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.{IContext,IData,IParameter,IVariable,IPrototype,DataModeMask, IDataSet}
import org.openmole.core.model.job.ITimeStamp
import org.openmole.core.model.task.{IGenericTask,IResource}
import org.openmole.core.model.execution.IProgress
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArraySeq
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object GenericTask {
  val Timestamps = new Data[ListBuffer[ITimeStamp]]("Timestamps#", classOf[ListBuffer[ITimeStamp]], Array(SYSTEM))
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
          case None =>  throw new UserBadDataError("Input data named \"" + p.name + "\" of type \"" + p.`type`.getName + "\" required by the task \"" + name + "\" has not been found");
          case Some(v) => 
            if (!p.isAssignableFrom(v.prototype)) throw new UserBadDataError("Input data named \"" + p.name + "\" required by the task \"" + name + "\" has the wrong type: \"" + v.prototype.`type`.getName + "\" instead of \"" + p.`type`.getName + "\" required")
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
          if (!d.mode.isOptional) {
            Logger.getLogger(classOf[GenericTask].getName).log(Level.WARNING, "Variable {0} of type {1} not found in output of task {2}.", Array[Object](p.name, p.`type`.getName, name))
          }
        case Some(v) =>
          
          if ( v.value == null || p.`type`.isAssignableFrom(v.value.asInstanceOf[AnyRef].getClass)) {
            vars += v
          } else {
            Logger.getLogger(classOf[GenericTask].getName).log(Level.WARNING, "Variable {0} of type {1} has been found but type doesn''t match : {2} in task {3}.",  Array[Object](p.name, p.`type`.getName, v.prototype.`type`.getName, name))
          }
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
  protected def process(global: IContext, context: IContext, progress: IProgress): Unit
   

  /* (non-Javadoc)
   * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
   */
  override def perform(global: IContext, context: IContext, progress: IProgress) = {
    try {
      deploy
      init(context)
      process(global, context, progress)
      end(context)
    } catch {
      case e => throw new InternalProcessingError(e, "Error in task " + name)
    }
  }

  private def end(context: IContext) = {
    filterOutput(context)
  }

  /* (non-Javadoc)
   * @see org.openmole.core.processors.ITask#addOutPrototype(org.openmole.core.data.structure.Prototype)
   */
  override def addOutput(prototype: IPrototype[_]): Unit = {
    addOutput(new Data(prototype))
  }

  override def addOutput(prototype: IPrototype[_], masks: Array[DataModeMask]): Unit = {
    addOutput(new Data(prototype, masks));
  }


  override def addOutput(data: IData[_]): Unit = {
    _outputs += ((data.prototype.name, data))
  }

  override def addResource(resource: IResource): Unit = {
    _resources += resource
  }

  override def addInput(prototype: IPrototype[_]): Unit = {
    addInput(new Data(prototype))
  }

  override def addInput(prototype: IPrototype[_], masks: Array[DataModeMask]): Unit = {
    addInput(new Data(prototype, masks))
  }


  override def addInput(data: IData[_]): Unit = {
     _inputs += ((data.prototype.name,data))
  }

  override def addInput(dataSet: IDataSet): Unit = {
    for(data <- dataSet) addInput(data)     
  }

  override def addOutput(dataSet: IDataSet) = {
    for(data <- dataSet) addOutput(data)
  }

  override def containsInput(name: String): Boolean = {
    _inputs.contains(name)
  }

  override def containsInput(prototype: IPrototype[_]): Boolean = {
    containsInput(prototype.name)
  }

  override def containsOutput(name: String): Boolean = {
    _outputs.contains(name)
  }

  override def containsOutput(prototype: IPrototype[_]): Boolean = {
    _outputs.contains(prototype.name)
  }

  override def inputs: IDataSet = new DataSet(_inputs)
    
  override def outputs: IDataSet = new DataSet(_outputs)
 
  //FIXME lazy val in scala 2.9.0 
  //@transient override lazy val 
  override def userInputs: IDataSet =  new DataSet(inputs.filter(!_.mode.isSystem))
   
  //@transient override lazy val 
  override def userOutputs: IDataSet = new DataSet(outputs.filter(!_.mode.isSystem))

  def resources: Iterable[IResource] = _resources
 
  override def deploy = {
    for (resource <- resources)  resource.deploy   
  }
    
  override def addParameter(parameter: IParameter[_]): Unit = {
    _parameters += ((parameter.variable.prototype.name,parameter))
  }
    
  override def addParameter[T](prototype: IPrototype[T] , value: T): Unit = addParameter(new Parameter[T](prototype, value))
    
  override def addParameter[T](prototype: IPrototype[T], value: T, `override`: Boolean): Unit = addParameter(new Parameter[T](prototype, value, `override`))
    
  override def parameters: Iterable[IParameter[_]]= _parameters.values
   
  override def toString: String = name       
    
}
