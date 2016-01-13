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

import org.openmole.core.workflow.domain._
import java.io.File

import org.openmole.core.workflow.tools.FromContext
import scala.reflect.runtime.universe._

package object modifier {

  implicit def domainModifierDecorator[D[_], T: TypeTag](domain: D[T])(implicit discrete: Discrete[D[T], T], inputs: DomainInputs[D[T]]) = new {
    def take(n: FromContext[Int]) = TakeDomain(domain, n)
    def group(n: FromContext[Int])(implicit m: Manifest[T]) = GroupDomain(domain, n)
    def sliding(n: FromContext[Int], s: FromContext[Int] = 1)(implicit m: Manifest[T]) = SlidingDomain(domain, n, s)
  }

  implicit def finiteDomainModifierDecorator[D[_], T](domain: D[T])(implicit finite: Finite[D[T], T], inputs: DomainInputs[D[T]]) = new {
    def sort(implicit o: Ordering[T]) = SortDomain(domain)
    def shuffle = ShuffleDomain(domain)
  }

  implicit class FileDomainDecorator[D](d: D)(implicit finite: Finite[D, File], inputs: DomainInputs[D]) {
    def sortByName = new SortByNameDomain(d)
  }

}