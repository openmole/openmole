/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.ParameterSet
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameter
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.task.ITask

abstract class TaskBuilder {
  private var _inputs: IDataSet = DataSet.empty
  private var _outputs: IDataSet = DataSet.empty
  private var _parameters: IParameterSet = ParameterSet.empty

  def addInput(d: IDataSet) = { _inputs ++= d; this }
  def addInput(d: IData[_]) = { _inputs += d; this }

  def addOutput(d: IDataSet) = { _outputs ++= d; this }
  def addOutput(d: IData[_]) = { _outputs += d; this }

  def addParameter(p: IParameter[_]) = { _parameters += p; this }
  def addParameter(p: IParameterSet) = { _parameters ++= p; this }

  def inputs = _inputs
  def outputs = _outputs
  def parameters = _parameters

  def toTask: ITask
}
