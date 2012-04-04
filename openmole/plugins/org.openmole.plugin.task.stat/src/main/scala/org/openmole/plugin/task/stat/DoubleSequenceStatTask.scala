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

package org.openmole.plugin.task.stat

import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

abstract class DoubleSequenceStatTask(name: String) extends Task(name){

  var sequences: List[(IPrototype[Array[Double]], IPrototype[Double])] = Nil

  def add(sequence: IPrototype[Array[Double]], stat: IPrototype[Double]): this.type = {
    addInput(sequence)
    addOutput(stat)
    sequences ::= (sequence, stat)
    this
  }
  
  def add(seqs: Iterable[(IPrototype[Array[Double]], IPrototype[Double])]): this.type = {
    seqs.foreach{ case(sequence, stat) => add(sequence, stat) }
    this
  }
  
  override def process(context: IContext) = 
    Context(
      sequences.map{
        case(sequence, statProto) => new Variable(statProto, stat(context.valueOrException(sequence)))
      }
    )
  
  def stat(seq: Array[Double]): Double
}
