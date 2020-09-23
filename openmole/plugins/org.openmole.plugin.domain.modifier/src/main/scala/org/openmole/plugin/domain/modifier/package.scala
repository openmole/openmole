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

import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._

import scala.reflect.runtime.universe._

package object modifier {

  trait CanGetName[A] {
    def getName(a: A): String
  }

  implicit val fileGetName = new CanGetName[File] { def getName(f: File) = f.getName }
  implicit val pathGetName = new CanGetName[Path] { def getName(p: Path) = p.toFile.getName }

  implicit def domainModifierDecorator[D, T: TypeTag](domain: D)(implicit discrete: DiscreteFromContext[D, T], inputs: DomainInputs[D]) = new {
    def take(n: FromContext[Int]) = TakeDomain(domain, n)
    def group(n: FromContext[Int])(implicit m: Manifest[T]) = GroupDomain(domain, n)
    def sliding(n: FromContext[Int], s: FromContext[Int] = 1)(implicit m: Manifest[T]) = SlidingDomain(domain, n, s)
    def map[O](f: T ⇒ O) = MapDomain[D, T, O](domain, f)
    def filter(f: T ⇒ Boolean) = FilteredDomain(domain, f)
    def zipWith[O](f: T ⇒ O) = ZipWithDomain[D, T, O](domain, f)
    def zipWithIndex = ZipWithIndexDomain[D, T](domain)
    def zipWithName(implicit cgn: CanGetName[T]) = zipWith(cgn.getName)
  }

  implicit def finiteDomainModifierDecorator[D, T](domain: D)(implicit finite: FiniteFromContext[D, T], inputs: DomainInputs[D]) = new {
    def sort(implicit o: Ordering[T]) = SortedByDomain(domain, identity[T])
    def sortBy[S: Ordering](s: T ⇒ S) = SortedByDomain(domain, s)
    def shuffle = ShuffleDomain(domain)
  }

  implicit def discreteFactorModifierDecorator[D, T: TypeTag](factor: Factor[D, T])(implicit discrete: DiscreteFromContext[D, T]) = new {
    def take(n: FromContext[Int]) = factor.copy(domain = factor.domain.take(n))
    def group(n: FromContext[Int])(implicit m: Manifest[T]) = factor.copy(domain = factor.domain.group(n))
    def sliding(n: FromContext[Int], s: FromContext[Int] = 1)(implicit m: Manifest[T]) = factor.copy(domain = factor.domain.sliding(n, s))
    def map[O](f: T ⇒ O) = factor.copy(domain = factor.domain.map(f))
    def filter(f: T ⇒ Boolean) = factor.copy(domain = factor.domain.filter(f))
    def zipWith[O](f: T ⇒ O) = factor.copy(domain = factor.domain.zipWith(f))
    def zipWithIndex = factor.copy(domain = factor.domain.zipWithIndex)
    def zipWithName(implicit cgn: CanGetName[T]) = factor.copy(domain = factor.domain.zipWithName)
  }

  implicit def FiniteFactorModifierDecorator[D, T](factor: Factor[D, T])(implicit finite: FiniteFromContext[D, T], inputs: DomainInputs[D]) = new {
    def sort(implicit o: Ordering[T]) = factor.copy(domain = factor.domain.sort)
    def sortBy[S: Ordering](s: T ⇒ S) = factor.copy(domain = factor.domain.sortBy(s))
    def shuffle = factor.copy(domain = factor.copy(domain = factor.domain.shuffle))
  }

}