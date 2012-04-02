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

package org.openmole.plugin.method.sensitivity

import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Context._

import SaltelliSampling._

class FirstOrderSensitivityTask(name: String, matrixName: IPrototype[String]) extends Task(name) {
  
  var modelInput: List[IPrototype[Double]] = Nil
  var modelOutput: List[IPrototype[Double]] = Nil
  
  addInput(toArray(matrixName))
  
  def addModelInput(p: IPrototype[Double]) = {
    modelInput ::= p
    addOutput(toArray(p))
  }
  
  def addModelOutput(p: IPrototype[Double]) = {
    modelOutput ::= p
    addInput(toArray(p))
  }
  
  override def process(context: IContext): IContext = {
    val matrixNames = context.valueOrException(toArray(matrixName))
    
    modelInput.map {
      input =>

      val sensitivity = 
        modelOutput.map {
          o => computeSensitivity(context.valueOrException(toArray(o)), matrixNames, input)
        }.toArray
      new Variable(toArray(input), sensitivity)
    }.toContext
  }
  
  def computeSensitivity(allValues: Array[Double], allNames: Array[String], input: IPrototype[Double]) = {
    val (a, b, c) = extractValues(allValues, allNames, input)
    val n = a.size
    
    val axcAvg = (a zip c map { case (a, c) => a * c } sum) / n
    val g0 = (a zip b map { case (a, b) => a * b } sum) / n
    val axaAvg = (a  map { a => a * a } sum) / n
    val f0 = (a sum) / n
    
    (axcAvg - g0) / (axaAvg - math.pow(f0, 2))
  }
    
  

  
}
