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

package org.openmole.plugin.domain

import java.io.File
import java.nio.file.Path
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

package object modifier:

  object CanGetName:
    implicit def fileGetName: CanGetName[File] = _.getName
    implicit def pathGetName: CanGetName[Path] = _.toFile.getName

  trait CanGetName[A]:
    def apply(a: A): String

  object DiscreteDomainModifiers:
    inline def derived[T]: DiscreteDomainModifiers[T] = new DiscreteDomainModifiers[T]

  class DiscreteDomainModifiers[D]

  implicit class OptInDomainModifierDecorator[D, T](domain: D)(using DiscreteFromContextDomain[D, T], DiscreteDomainModifiers[D]):
    def take(n: FromContext[Int]) = TakeDomain[D, T](domain, n)
    def group(n: FromContext[Int])(using Manifest[T]) = GroupDomain(domain, n)
    def sliding(n: FromContext[Int], s: FromContext[Int] = 1)(using Manifest[T]) = SlidingDomain(domain, n, s)

    def map[O](f: T => O) = MapDomain[D, T, O](domain, f)
    def map[O: Manifest](s: String)(using Manifest[T]) = MapDomain[D, T, O](domain, FromContext.codeToFromContext[T => O](s))

    def filter(f: T => Boolean) = FilteredDomain(domain, f)
    def zipWithIndex = ZipWithIndexDomain[D, T](domain)
    def sort(using Ordering[T]) = SortedByDomain(domain, identity[T])
    def sortBy[S: Ordering](s: T => S) = SortedByDomain(domain, s)
    def distinct = DistinctDomain(domain)
    def takeWhile(predicate: T => Boolean) = TakeWhileDomain(domain, predicate)

    def zipWith[O](f: T => O) = ZipWithDomain[D, T, O](domain, f)
    def zipWith[O: Manifest](f: String)(using Manifest[T]) = ZipWithDomain[D, T, O](domain, FromContext.codeToFromContext[T => O](f))
    def zipWithName(using cgn: CanGetName[T]) = zipWith(cgn.apply)

    def sortBy[S: {Ordering, Manifest}](s: String)(using Manifest[T]) = SortedByDomain(domain, FromContext.codeToFromContext[T => S](s))
    def takeWhile(predicate: String)(using Manifest[T]) = TakeWhileDomain(domain, FromContext.codeToFromContext[T => Boolean](predicate))
    def shuffle = ShuffleDomain(domain)
    def ++[D2](d2: D2)(using DiscreteFromContextDomain[D2, T]) = AppendDomain(domain, d2)


//  implicit class DomainModifierDecorator[D, T](domain: D)(using DiscreteFromContextDomain[D, T]):



//  implicit def discreteFactorModifierDecorator[D, T: TypeTag](factor: Factor[D, T])(implicit discrete: DiscreteFromContext[D, T]) = new {
  //    def take(n: FromContext[Int]) = factor.copy(domain = factor.domain.take(n))
  //    def group(n: FromContext[Int])(implicit m: Manifest[T]) = factor.copy(domain = factor.domain.group(n))
  //    def sliding(n: FromContext[Int], s: FromContext[Int] = 1)(implicit m: Manifest[T]) = factor.copy(domain = factor.domain.sliding(n, s))
  //    def map[O](f: T => O) = factor.copy(domain = factor.domain.map(f))
  //    def filter(f: T => Boolean) = factor.copy(domain = factor.domain.filter(f))
  //    def zipWith[O](f: T => O) = factor.copy(domain = factor.domain.zipWith(f))
  //    def zipWithIndex = factor.copy(domain = factor.domain.zipWithIndex)
  //    def zipWithName(implicit cgn: CanGetName[T]) = factor.copy(domain = factor.domain.zipWithName)
  //  }
  //
  //  implicit def finiteFactorModifierDecorator[D, T](factor: Factor[D, T])(implicit finite: FiniteFromContext[D, T], inputs: DomainInputs[D]) = new {
  //    def sort(implicit o: Ordering[T]) = factor.copy(domain = factor.domain.sort)
  //    def sortBy[S: Ordering](s: T => S) = factor.copy(domain = factor.domain.sortBy(s))
  //    def shuffle = factor.copy(domain = factor.copy(domain = factor.domain.shuffle))
  //    def distinct = factor.copy(domain = factor.domain.distinct)
  //    def takeWhile(predicate: FromContext[T => Boolean]) = factor.copy(domain = factor.domain.takeWhile(predicate))
  //  }

