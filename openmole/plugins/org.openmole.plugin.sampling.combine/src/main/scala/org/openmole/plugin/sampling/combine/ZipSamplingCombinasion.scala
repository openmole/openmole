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

package org.openmole.plugin.sampling.combine

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.ISampling
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

class ZipSamplingCombinasion(reference: ISampling, samplings: Iterable[ISampling]) extends ISampling {

  def this(reference: ISampling) = this(reference, Iterable.empty) 
    
  def this(reference: ISampling, head: IFactor[T,IDomain[T]] forSome{type T}, factors: Array[IFactor[T,IDomain[T]] forSome{type T}]) = this(reference, (List(head) ++ factors).map{new FactorSamplingAdapter(_)})

  def this(reference: IFactor[T,IDomain[T]] forSome{type T}, factors: Array[IFactor[T,IDomain[T]] forSome{type T}]) = this(new FactorSamplingAdapter(reference), factors.map{new FactorSamplingAdapter(_)})

  def this(reference: ISampling, head: ISampling, samplings: Array[ISampling]) = this(reference, List(head) ++ samplings) 

  override def build(global: IContext, context: IContext): Iterable[Iterable[IVariable[_]]] = {

    /* Compute plans */
    val cachedSample = new ArrayBuffer[Iterator[Iterable[IVariable[_]]]](samplings.size)

    for(otherSampler <- samplings) {
      cachedSample += otherSampler.build(global, context).iterator
    }

    /* Compose plans */
    val factorValuesCollection = new ListBuffer[Iterable[IVariable[_]]]

    val valuesIterator = reference.build(global, context).iterator
    var oneFinished = false

    while(valuesIterator.hasNext && !oneFinished) {
      val values = new ListBuffer[IVariable[_]]
      
      for(it <- cachedSample) {
        if(!it.hasNext)  oneFinished = true
        else values ++= (it.next)
      }

      if(!oneFinished) {
        values ++= (valuesIterator.next)
        factorValuesCollection +=  values
      }
    }

    return factorValuesCollection
  }



}
