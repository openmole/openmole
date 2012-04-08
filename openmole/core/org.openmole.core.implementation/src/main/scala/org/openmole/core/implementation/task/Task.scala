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

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Parameter
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data.{IContext,IData,IParameter,IVariable,IPrototype,DataModeMask, IDataSet}
import org.openmole.core.model.task.ITask
import org.openmole.core.implementation.data.Context._
import scala.collection.immutable.TreeSet
import java.io.File
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.pluginmanager.PluginManagerInfo
import org.openmole.misc.tools.io.FileUtil.fileOrdering

abstract class Task extends ITask {
  
  import Data._
  import Parameter._
  
  private var _inputs = List.empty[IData[_]]
  private var _outputs = List.empty[IData[_]]
  private var _parameters = List.empty[IParameter[_]]
  private var _plugins = new TreeSet[File]
  
  protected def verifyInput(context: IContext) = {
    for (d <- inputs) {
      if (!(d.mode is optional)) {
        val p = d.prototype
        context.variable(p.name) match {
          case None => throw new UserBadDataError("Input data named \"" + p.name + "\" of type \"" + p.`type`.toString + "\" required by the task \"" + name + "\" has not been found");
          case Some(v) => if (!p.isAssignableFrom(v.prototype)) throw new UserBadDataError("Input data named \"" + p.name + "\" required by the task \"" + name + "\" has the wrong type: \"" + v.prototype.`type`.toString + "\" instead of \"" + p.`type`.toString + "\" required")
        }
      }
    }
    context
  }

  protected def filterOutput(context: IContext): IContext =
    outputs.flatMap {
      d => val p = d.prototype
      context.variable(p) match {
        case None => 
          if (!(d.mode is optional)) throw new UserBadDataError("Variable " + p.name + " of type " + p.`type`.toString +" in not optional and has not found in output of task " + name +".")
          else Option.empty[IVariable[_]]
        case Some(v) =>
          if (p.accepts(v.value)) Some(v)
          else throw new UserBadDataError("Output value of variable " + p.name + " (prototype: "+ v.prototype.`type`.toString +") is instance of class '" + v.value.asInstanceOf[AnyRef].getClass + "' and doesn't match the expected class '" + p.`type`.toString + "' in task" + name + ".")
      }
    }.toContext

  private def init(context: IContext): IContext = {
    if(PluginManagerInfo.enabled) PluginManager.loadIfNotAlreadyLoaded(plugins) 
    else if(!plugins.isEmpty) throw new InternalProcessingError("Plugins can't be loadded cause the application isn't runned in an osgi environment.")

    verifyInput(
      context ++ 
        parameters.flatMap {
          parameter =>
            if (parameter.`override` || !context.contains(parameter.variable.prototype.name)) Some(parameter.variable)
            else Option.empty[IVariable[_]]
        }
    )
  }

  private def end(context: IContext) = filterOutput(context)
  
  /**
   * The main operation of the processor.
   * @param context
   * @param progress
   */
  @throws(classOf[Throwable])
  protected def process(context: IContext): IContext
   

  /* (non-Javadoc)
   * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
   */
  override def perform(context: IContext) = {
    try end(context + process(init(context)))
    catch {
      case e => throw new InternalProcessingError(e, "Error in task " + name)
    }
  }

  def addOutput(prototype: IPrototype[_]): this.type = addOutput(new Data(prototype))

  def addOutput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type = addOutput(new Data(prototype, masks)); this
 
  def addOutput(data: IData[_]): this.type =  {
    _outputs ::= data
    this
  }
  
  def addInput(prototype: IPrototype[_]): this.type = addInput(new Data(prototype))

  def addInput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type = addInput(new Data(prototype, masks))

  def addInput(data: IData[_]): this.type = {
    _inputs ::= data
    this
  }

  def addInput(dataSet: IDataSet): this.type = {
    for(data <- dataSet) addInput(data)     
    this
  }

  def addOutput(dataSet: IDataSet): this.type = {
    for(data <- dataSet) addOutput(data)
    this
  }
  
  override def inputs: IDataSet = new DataSet(_inputs.toList)
  override def outputs: IDataSet = new DataSet(_outputs.toList)
    
  def addParameter(parameter: IParameter[_]): this.type = {
    _parameters ::= parameter
    this
  }
    
  def addParameter[T](prototype: IPrototype[T] , value: T): this.type = addParameter(new Parameter[T](prototype, value))
    
  def addParameter[T](prototype: IPrototype[T], value: T, `override`: Boolean): this.type = addParameter(new Parameter[T](prototype, value, `override`))
    
  override def parameters: Iterable[IParameter[_]]= _parameters
   
  def addPlugin(plugin: File): this.type = {_plugins += plugin; this}
  def addPlugin(plugin: String): this.type = addPlugin(new File(plugin))
  
  override def plugins = _plugins
  
  override def toString: String = name       
    
}
