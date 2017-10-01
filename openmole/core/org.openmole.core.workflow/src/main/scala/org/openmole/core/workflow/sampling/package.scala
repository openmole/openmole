/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow

package sampling {

  import org.openmole.core.context._
  import org.openmole.core.expansion._
  import org.openmole.tool.types._
  import org.openmole.core.workflow.domain._
  import cats.implicits._

  trait SamplingPackage {

    implicit class PrototypeFactorDecorator[T](p: Val[T]) {
      def in[D](d: D): Factor[D, T] = Factor(p, d)
      def is(d: FromContext[T]) = Factor(p, d)
    }

    implicit def fromContextIsFinite[T] = new Finite[FromContext[T], T] {
      override def computeValues(domain: FromContext[T]): FromContext[Iterable[T]] =
        domain.map(v ⇒ Vector(v))
    }

    implicit def tupleOfStringToBounds[T: FromString: Manifest] = new Bounds[(String, String), T] {
      override def min(domain: (String, String)): FromContext[T] = FromContext.codeToFromContext[T](domain._1)
      override def max(domain: (String, String)): FromContext[T] = FromContext.codeToFromContext[T](domain._2)
    }

    implicit def tupleIsBounds[T] = new Bounds[(T, T), T] {
      override def min(domain: (T, T)) = domain._1
      override def max(domain: (T, T)) = domain._2
    }

    implicit def iterableOfTuplesIsBounds[T: Manifest] = new Bounds[Iterable[(T, T)], Array[T]] {
      override def min(domain: Iterable[(T, T)]) = FromContext.value(domain.unzip._1.toArray)
      override def max(domain: Iterable[(T, T)]) = FromContext.value(domain.unzip._2.toArray)
    }

    implicit def iterableOfStringTuplesIsBounds[T: FromString: Manifest] = new Bounds[Iterable[(String, String)], Array[T]] {
      override def min(domain: Iterable[(String, String)]) = domain.unzip._1.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
      override def max(domain: Iterable[(String, String)]) = domain.unzip._2.toVector.traverse(v ⇒ FromContext.codeToFromContext[T](v)).map(_.toArray)
    }

    implicit def discreteFactorIsSampling[D, T](f: Factor[D, T])(implicit discrete: Discrete[D, T]) = FactorSampling(f)
  }
}

package object sampling