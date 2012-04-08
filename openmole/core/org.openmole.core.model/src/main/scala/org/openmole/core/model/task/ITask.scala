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
   * Get all the parameters configured for this task.
   *
   * @return the parameters configured for this task.
   */
  def parameters: Iterable[IParameter[_]]
  
  
  def plugins: Iterable[File]
  
  def addInput(data: IData[_]): this.type
  def addInput(prototype: IPrototype[_]): this.type
  def addOutput(data: IData[_]): this.type
  def addOutput(prototype: IPrototype[_]): this.type
  def addParameter(parameter: IParameter[_]): this.type
}
