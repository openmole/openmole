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
import org.openmole.tool.types.{ FromArray, ToDouble }

object AggregateTask {

  object AggregateVal {
    implicit def fromAggregateToDouble[A: ToDouble, B: Manifest, V[_]: FromArray](a: Aggregate[Val[A], V[Double] ⇒ B]) = AggregateVal.applyToDouble(a, a.value.withType[B])
    implicit def fromAggregate[A, B: Manifest, V[_]: FromArray](a: Aggregate[Val[A], V[A] ⇒ B]) = AggregateVal(a, a.value.withType[B])

    implicit def fromAsAggregateToDouble[A: ToDouble, B: Manifest, V[_]: FromArray](as: As[Aggregate[Val[A], V[Double] ⇒ B], Val[B]]) = AggregateVal.applyToDouble(as.value, as.as)
    implicit def fromAsAggregate[A, B: Manifest, V[_]: FromArray](as: As[Aggregate[Val[A], V[A] ⇒ B], Val[B]]) = AggregateVal(as.value, as.as)

    implicit def fromAsStringAggregateToDouble[A: ToDouble, B: Manifest, V[_]: FromArray](as: As[Aggregate[Val[A], V[Double] ⇒ B], String]) = AggregateVal.applyToDouble(as.value, Val[B](as.as))
    implicit def fromAsStringAggregate[A, B: Manifest, V[_]: FromArray](as: As[Aggregate[Val[A], V[A] ⇒ B], String]) = AggregateVal(as.value, Val[B](as.as))

    implicit def fromVal[A: Manifest](v: Val[A]) = AggregateVal[A, Array[A], Array](v aggregate identity _, v.toArray)
    implicit def fromAsVal[A: Manifest](v: As[Val[A], Val[Array[A]]]) = AggregateVal[A, Array[A], Array](Aggregate[Val[A], Array[A] ⇒ Array[A]](v.value, identity), v.as)
    implicit def fromAsValString[A: Manifest](v: As[Val[A], String]) = AggregateVal[A, Array[A], Array](Aggregate[Val[A], Array[A] ⇒ Array[A]](v.value, identity), Val[Array[A]](v.as))

    def apply[A, B: Manifest, V[_]: FromArray](a: Aggregate[Val[A], V[A] ⇒ B], _outputVal: Val[B]): AggregateVal[A, B] = new AggregateVal[A, B] {
      def aggregate(context: Context): Variable[B] = {
        val fromArray = implicitly[FromArray[V]]
        Variable(outputVal, a.aggregate(fromArray(context.apply(a.value.toArray))))
      }
      def outputVal: Val[B] = _outputVal
      def value: Val[A] = a.value
    }

    def applyToDouble[A: ToDouble, B: Manifest, V[_]: FromArray](a: Aggregate[Val[A], V[Double] ⇒ B], _outputVal: Val[B]): AggregateVal[A, B] = new AggregateVal[A, B] {
      def aggregate(context: Context): Variable[B] = {
        val fromArray = implicitly[FromArray[V]]
        val toDouble = implicitly[ToDouble[A]]
        Variable(outputVal, a.aggregate(fromArray(context.apply(a.value.toArray).map(toDouble.apply))))
      }
      def outputVal: Val[B] = _outputVal
      def value: Val[A] = a.value
    }

  }

  trait AggregateVal[A, B] {
    def aggregate(context: Context): Variable[B]
    def outputVal: Val[B]
    def value: Val[A]
  }

  def apply(aggregates: Seq[AggregateVal[_, _]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("AggregateTask") { p ⇒
      import p._
      context ++ aggregates.map { case a ⇒ a.aggregate(context) }
    } set (
      inputs += (aggregates.map(_.value.toArray): _*),
      outputs += (aggregates.map(_.outputVal): _*)
    )

}
