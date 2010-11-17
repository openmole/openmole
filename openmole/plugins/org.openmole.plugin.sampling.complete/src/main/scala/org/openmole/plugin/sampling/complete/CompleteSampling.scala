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

package org.openmole.plugin.sampling.complete

import java.util.logging.Logger
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ArraySeq
import scala.util.control.Breaks._ 

class CompleteSampling(factors: Iterable[(IFactor[T,IDomain[T]]) forSome{type T}]) extends Sampling(factors) {

  def this(factors : Array[(IFactor[T,IDomain[T]]) forSome{type T}]) = this(factors.toIterable)

  override def build(global: IContext, context: IContext): Iterable[Iterable[IVariable[_]]] = {
    var size = 1
        
    val values = (for (factor <- factors) yield {
        val factorValue = (factor.prototype, factor.domain.iterator(global, context).toIndexedSeq)
        if(factorValue._2.size == 0) return Iterable.empty
        size *= factorValue._2.size
        factorValue
      }).toIndexedSeq

    val curIndexes = new ArraySeq[Int](factors.size)
    
    val iterators = values.map{_._2.iterator}
    val listOfListOfValues = new ArrayBuffer[Iterable[IVariable[_]]](size)

    var end = false
    // Fetching each list of values
    while (!end) {

      // Append the values to the list
      val factorValues = new ArrayBuffer[IVariable[_]](factors.size)

      for (i <- 0 until factors.size) {
        val curVal = values(i)
        //FIXME downcasting
        factorValues += new Variable(curVal._1.asInstanceOf[IPrototype[Any]], curVal._2(curIndexes(i)))
      }

      listOfListOfValues += factorValues

      // preparing the next value
      breakable {
        for (i <- 0 until factors.size) {
          if (curIndexes(i) < values(i)._2.size - 1) {
            curIndexes(i) += 1
            break
          } else {
            if (i == factors.size - 1) {
              // The last vector has no more values, so the loop is ended
              end = true
              break
            }
            // else, we reset the current vector, and let increment the next vector
            curIndexes(i) = 0
          }
        } }
    }
 
    listOfListOfValues
  }

}
