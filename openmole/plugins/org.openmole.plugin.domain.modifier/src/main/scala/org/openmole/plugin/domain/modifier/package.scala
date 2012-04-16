/*
 * Copyright (C) 2012 reuillon
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

import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable

package object modifier {
  
  implicit def domainModifierDecorator[T](domain: IDomain[T] with IIterable[T]) = new {
    def take(n: Int) = new TakeDomain(domain, n)
    def group(n: Int)(implicit m: Manifest[T]) = new GroupDomain(domain, n)
  }
  
}