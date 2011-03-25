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

  def this(factors: Array[(IFactor[T,IDomain[T]]) forSome{type T}]) = this(factors.toIterable)

  def this(factors: IFactor[T,IDomain[T]] forSome{type T}*) = this(factors.toIterable)

  override def build(context: IContext): Iterable[Iterable[IVariable[_]]] = {
    val values = factors.map{f => f.domain.iterator(context).toIterable}
    val composed = values.view.map{_.map(e => List(e))}.reduceRight((v1, v2) => v1.flatMap(e1 => v2.map{e2 =>  e1 ::: e2}))
    composed.map{comp => factors.zip(comp).map{case (f,v) => new Variable(f.prototype.asInstanceOf[IPrototype[Any]], v)}}
  }

}
