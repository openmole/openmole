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

package org.openmole.core.model.task

import org.openmole.core.model.data.{IParameter,IPrototype,IDataSet, IData, DataModeMask}
import java.io.File
import org.openmole.core.model.data.IContext

trait ITask {
   /**
   *
   * Perform this task.
   *
   * @param context the context in which the task will be executed
   */
  def perform(context: IContext): IContext

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
   * Add <code>data</code> as an input for this task.
   *
   * @param data the data added in input
   */
  def addInput(data: IData[_]): this.type
    
  def addInput(dataSet: IDataSet): this.type

  def addInput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type

  /**
   *
   * Add a non optional data constructed from <code>prototype</code> as an input for this task.
   *
   * @param prototype the prototype of the data
   */
  def addInput(prototype: IPrototype[_]): this.type

  //def containsInput(name: String): Boolean
  //def containsInput(name: IPrototype[_]): Boolean


  /**
   *
   * Add <code>data</code> as an output for this task.
   *
   * @param data the data to add
   */
  def addOutput(data: IData[_]): this.type
  def addOutput(dataSet: IDataSet): this.type

  def addOutput(prototype: IPrototype[_], masks: Array[DataModeMask]): this.type

  /**
   *
   * Add a non optional data constructed from <code>prototype</code> as an output for this task.
   *
   * @param prototype prototype the prototype of the data
   */
  def addOutput(prototype: IPrototype[_]): this.type

  //def containsOutput(name: String): Boolean
  //def containsOutput(name: IPrototype[_]): Boolean
   


  /**
   *
   * Add a parameter for this task.
   *
   * @param parameter     the parameter to add
   */
  def addParameter(parameter: IParameter[_]): this.type

  /**
   *
   * Add a parameter for this task.
   *
   * @param <T> a super type type of the parameter
   * @param prototype     the prototype of the parameter
   * @param value         the value of the parameter
   */
  def addParameter[T](prototype: IPrototype[T], value: T): this.type

  /**
   *
   * Add a parameter for this task.
   *
   * @param <T> a super type type of the parameter
   * @param prototype prototype the prototype of the parameter
   * @param value         value the value of the parameter
   * @param override      true if the parameter should override an existing value
   */
  def addParameter[T](prototype: IPrototype[T], value: T, `override`: Boolean): this.type
  
  /**
   *
   * Get all the parameters configured for this task.
   *
   * @return the parameters configured for this task.
   */
  def parameters: Iterable[IParameter[_]]
  
  def addPlugin(plugin: File): this.type
  def addPlugin(plugin: String): this.type
  def plugins: Iterable[File]
}
