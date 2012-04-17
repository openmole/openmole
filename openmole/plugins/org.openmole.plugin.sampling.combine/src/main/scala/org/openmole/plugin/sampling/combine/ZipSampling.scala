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

package org.openmole.plugin.sampling.combine

import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.ISampling
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

class ZipSampling(samplings: Iterable[ISampling]) extends ISampling {

  def this(samplings: ISampling*) = this(samplings)
  def this(head: ISampling, samplings: Array[ISampling]) = this(List(head) ++ samplings) 

  override def inputs = DataSet.empty ++ samplings.flatMap(_.inputs)
  override def prototypes = samplings.flatMap(_.prototypes)
  
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = 
    samplings.headOption match {
      case Some(reference) =>
        /* Compute plans */
        val cachedSample = samplings.tail.map{_.build(context)}.toArray

        /* Compose plans */
        val factorValuesCollection = new ListBuffer[Iterable[IVariable[_]]]

        val valuesIterator = reference.build(context)
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

        factorValuesCollection.iterator
  
      case None => Iterator.empty
    }


}
