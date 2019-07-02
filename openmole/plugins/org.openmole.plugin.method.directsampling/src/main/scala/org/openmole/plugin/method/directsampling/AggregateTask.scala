package org.openmole.plugin.method.directsampling

/*
 * Copyright (C) 2019 Romain Reuillon
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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object AggregateTask {

  object AggregateVal {
    implicit def fromAggregate[A, B: Manifest](a: Aggregate[Val[A], Array[A] ⇒ B]) = AggregateVal(a, a.value.withType[B])
    implicit def fromInAggregate[A, B: Manifest](in: In[Aggregate[Val[A], Array[A] ⇒ B], Val[B]]) = AggregateVal(in.value, in.domain)
    implicit def fromVal[A: Manifest](v: Val[A]) = AggregateVal[A, Array[A]](v aggregate identity, v.withType[Array[A]])
    implicit def fromInVal[A: Manifest](v: In[Val[A], Val[Array[A]]]) = AggregateVal[A, Array[A]](Aggregate(v.value, identity), v.domain)
  }

  case class AggregateVal[A, B: Manifest](a: Aggregate[Val[A], Array[A] ⇒ B], outputVal: Val[B]) {
    def aggregate(context: Context): Variable[B] = Variable(outputVal, a.aggregate(context.apply(a.value.toArray)))
  }

  def apply(aggregates: AggregateVal[_, _]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("AggregateTask") { p ⇒
      import p._
      context ++ aggregates.map { case a ⇒ a.aggregate(context) }
    } set (
      inputs += (aggregates.map(_.a.value.toArray): _*),
      outputs += (aggregates.map(_.outputVal): _*)
    )

}
