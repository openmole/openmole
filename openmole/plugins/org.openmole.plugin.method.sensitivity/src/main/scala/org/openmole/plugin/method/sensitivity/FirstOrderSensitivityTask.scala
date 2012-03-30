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
  
  override def process(context: IContext): IContext = 
    modelInput.map {
      input =>
      val matrixNames = context.valueOrException(toArray(matrixName))
        
      val sensitivity = 
        modelOutput.map {
          o => computeSensitivity(matrixNames, context.valueOrException(toArray(o)), input)
        }.toArray
      new Variable(toArray(input), sensitivity)
    }.toContext
 
  
  def computeSensitivity(allNames: Array[String], allOutputValues: Array[Double], input: IPrototype[Double]) = {
    val outputCValues = extractValues(allOutputValues, allNames, cMatrixName(input.name))
    val outputAValues = extractValues(allOutputValues, allNames, aMatrixName)
    val outputBValues = extractValues(allOutputValues, allNames, bMatrixName)
    val n = outputAValues.size
    
    val axcAvg = (outputAValues zip outputCValues map { case (a, c) => a * c } sum) / n
    val g0 = (outputAValues zip outputBValues map { case (a, b) => a * b } sum) / n
    val axaAvg = (outputAValues  map { a => a * a } sum) / n
    val f0 = (outputAValues sum) / n
    
    (axcAvg - g0) / (axaAvg - math.pow(f0, 2))
  }
    
  
  def extractValues(allValues: Array[Double], allNames: Array[String], name: String): Array[Double] = 
    allValues zip allNames filter { case(_, n) => n == name } map { case(v, _) => v }
  
}
