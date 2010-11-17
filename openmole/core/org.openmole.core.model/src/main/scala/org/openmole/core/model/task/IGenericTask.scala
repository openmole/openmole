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

package org.openmole.core.model.task


/**
 *
 * Generic interface for all tasks in OpenMOLE.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
import org.openmole.core.model.data.{IParameter,IPrototype,IDataSet, IData, DataModeMask}
import org.openmole.core.model.data.IContext
import org.openmole.core.model.execution.IProgress


trait IGenericTask {

  /**
   *
   * Perform this task.
   *
   * @param context the context in which it is executed
   * @param progress the progress reporting structure of the task
   */
  def perform(global: IContext, context: IContext, progress: IProgress)

  /**
   *
   * Get the name of the task.
   *
   * @return
   */
  def name: String

  /**
   *
   * Get the input data of the task.
   *
   * @return the input of the task
   */
  def inputs: IDataSet

  /**
   *
   * Get the output data of the task.
   *
   * @return the output data of the task
   */
  def outputs: IDataSet

  /**
   *
   * Add <code>resource</code> to this task.
   *
   * @param ressource the resource to be added
   */
  def addResource(resource: IResource)
  
  /**
   *
   * Deploy the resource locally.
   *
   * @throws InternalProcessingError something went wrong for a reason that can't be directly related to a user defined parameter
   * @throws UserBadDataError something went wrong for a reason that can be directly related to a user defined parameter
   */
  def deploy

  /**
   *
   * Add <code>data</code> as an input for this task.
   *
   * @param data the data added in input
   */
  def addInput(data: IData[_]) 
    
  def addInput(dataSet: IDataSet)

  def addInput(prototype: IPrototype[_], masks: DataModeMask*)

  /**
   *
   * Add a non optional data constructed from <code>prototype</code> as an input for this task.
   *
   * @param prototype the prototype of the data
   */
  def addInput(prototype: IPrototype[_])

  def containsInput(name: String): Boolean
  def containsInput(name: IPrototype[_]): Boolean


  /**
   *
   * Add <code>data</code> as an output for this task.
   *
   * @param data the data to add
   */
  def addOutput(data: IData[_])
  def addOutput(dataSet: IDataSet)

  def addOutput(prototype: IPrototype[_], masks: DataModeMask*)

  /**
   *
   * Add a non optional data constructed from <code>prototype</code> as an output for this task.
   *
   * @param prototype prototype the prototype of the data
   */
  def addOutput(prototype: IPrototype[_])

  def containsOutput(name: String): Boolean
  def containsOutput(name: IPrototype[_]): Boolean
   


  /**
   *
   * Add a parameter for this task.
   *
   * @param parameter     the parameter to add
   */
  def addParameter(parameter: IParameter[_])

  /**
   *
   * Add a parameter for this task.
   *
   * @param <T> a super type type of the parameter
   * @param prototype     the prototype of the parameter
   * @param value         the value of the parameter
   */
  def addParameter[T](prototype: IPrototype[T], value: T)

  /**
   *
   * Add a parameter for this task.
   *
   * @param <T> a super type type of the parameter
   * @param prototype prototype the prototype of the parameter
   * @param value         value the value of the parameter
   * @param override      true if the parameter should override an existing value
   */
  def addParameter[T](prototype: IPrototype[T], value: T, `override`: Boolean)
  
  /**
   *
   * Get all the parameters configured for this task.
   *
   * @return the parameters configured for this task.
   */
  def parameters: Iterable[IParameter[_]]
    
  def userInputs: IDataSet  
   
  def userOutputs: IDataSet

}
