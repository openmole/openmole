package org.openmole.plugin.domain.modifier

/*
 * Copyright (C) 2021 Romain Reuillon
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
import org.openmole.core.workflow.domain
import org.openmole.plugin.domain.range._
import org.openmole.plugin.domain.file._
import org.scalatest._

class ModifierDomainSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers:
  import org.openmole.core.workflow.test.*
  import Stubs.*

  "inputs of modified domain" should "be as expected" in:
    val size = Val[Int]
    val range = RangeDomain[Int](0, 10, 1)
    val take = range.take(size)
    TakeDomain.isDiscrete(take).inputs should contain(size)

  "Domains" should "be serializable" in :
    val size = Val[Int]
    val range = RangeDomain[Int](0, 10, 1)

    val take = serializeDeserialize(TakeDomain(range, size))
    
    TakeDomain.isDiscrete(take).inputs should contain(size)
    TakeDomain.isDiscrete(take).domain.from(Context(size -> 3)).toVector should contain(Vector(0, 1, 2))

  "range" should "work with modifiers" in:
    RangeDomain[Double](0.0, 10.0, 0.1).map(x ⇒ x * x)
    RangeDomain[Int](0, 10).map(x ⇒ x * x)

    val range = RangeDomain[Int](0, 10, 1)
    val take = range.take(10)
    val t = Val[Int]
    val sampling: Sampling = t in take

  "files" should "work with modifiers" in:
    val f = Val[File]

    f in (File("/tmp").files().filter(f ⇒ f.getName.startsWith("exp") && f.getName.endsWith(".csv")))

