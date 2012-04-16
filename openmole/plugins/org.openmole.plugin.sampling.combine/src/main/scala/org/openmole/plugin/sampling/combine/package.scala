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

package org.openmole.plugin.sampling

import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.sampling.ISampling

package object combine {
  
  implicit def combineSamplingDecorator(s: ISampling) = new {
    def x(s2: ISampling) = new CompleteSampling(s, s2)
    def zip(s2: ISampling) = new Zip(s, s2)
    def zipWithIndex(index: IPrototype[Int]) = new ZipWithIndex(s, index)
  }
  
  implicit def zipWithNameFactorDecorator(factor: IFactor[File, IDomain[File] with IIterable[File]]) = new {
    def zipWithName(name: IPrototype[String]) = new ZipWithName(factor, name)
  }
  
}