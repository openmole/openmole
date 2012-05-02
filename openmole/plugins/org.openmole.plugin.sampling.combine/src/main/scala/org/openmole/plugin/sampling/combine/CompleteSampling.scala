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
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.ISampling
import scala.util.control.Breaks._

sealed class CompleteSampling(samplings: ISampling*) extends ISampling {

  override def inputs = DataSet.empty ++ samplings.flatMap { _.inputs }
  override def prototypes: Iterable[IPrototype[_]] = samplings.flatMap { _.prototypes }

  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] =
    if (samplings.isEmpty) Iterator.empty
    else {
      val values = samplings.map { _.build(context).toList }
      val composed = values.view.reduce { (a, b) ⇒ combine(a, b) }
      composed.iterator //map{comp => factors.zip(comp).map{case (f,v) => new Variable(f.prototype.asInstanceOf[IPrototype[Any]], v)}}.iterator
    }

  def combine[A](it1: List[Iterable[A]], it2: List[Iterable[A]]): List[Iterable[A]] =
    for (v1 ← it1; v2 ← it2) yield v1 ++ v2

}
