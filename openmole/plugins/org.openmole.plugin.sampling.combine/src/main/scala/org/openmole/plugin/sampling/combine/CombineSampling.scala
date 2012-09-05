/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.sampling.combine

import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._

class CombineSampling(samplings: ISampling*) extends ISampling {

  override def inputs = DataSet.empty ++ samplings.flatMap { _.inputs }
  override def prototypes: Iterable[Prototype[_]] = samplings.flatMap { _.prototypes }

  override def build(context: Context): Iterator[Iterable[Variable[_]]] =
    if (samplings.isEmpty) Iterator.empty
    else
      samplings.tail.foldLeft(samplings.head.build(context)) {
        (a, b) ⇒ combine(a, b, context)
      }

  def combine(s1: Iterator[Iterable[Variable[_]]], s2: ISampling, context: Context) =
    for (x ← s1; y ← s2.build(context ++ x)) yield x ++ y

}
