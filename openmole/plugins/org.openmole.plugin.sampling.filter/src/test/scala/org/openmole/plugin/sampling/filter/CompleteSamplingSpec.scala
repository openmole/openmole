/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.sampling.filter

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.data._

import org.openmole.core.model.sampling.ISampling
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class FiltredSamplingSpec extends FlatSpec with ShouldMatchers {

  "Filtred sampling" should "remove all value which doesn't match the filters" in {

    val p1 = Prototype[Int]("p1")
    val p2 = Prototype[Int]("p2")
    val p3 = Prototype[Int]("p3")

    def pList(i: Int, j: Int, k: Int) = List(i, j, k).zip(List(p1, p2, p3)).map { case (v, p) ⇒ Variable(p, v) }

    val sampling = new Sampling {
      override def prototypes = List(p1, p2, p3)
      override def build(context: Context) = List(pList(1, 2, 3), pList(4, 3, 4), pList(1, 5, 3), pList(2, 3, 4), pList(6, 7, 8)).iterator
    }

    val f1 = new IFilter {
      override def apply(factorsValues: Context) = factorsValues.value(p1).get != 1
    }

    val f2 = new IFilter {
      override def apply(factorsValues: Context) = factorsValues.value(p3).get < 5
    }

    val s2 = new FiltredSampling(sampling, f1, f2).build(Context.empty)
    s2.size should equal(2)
  }

}
