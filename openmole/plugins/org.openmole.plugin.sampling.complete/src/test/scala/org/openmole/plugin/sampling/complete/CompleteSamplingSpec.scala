/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.sampling.complete

import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Context
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class CompleteSamplingSpec extends FlatSpec with ShouldMatchers {
  
  "Complete sampling" should "combine all factor values with each others" in {
    
    val r1 = (1 to 3)
    val r2 = (1 to 5)
    val r3 = (1 to 2)
    
    val f1 = new Factor(new Prototype("i", classOf[Int]), new IDomain[Int] {
        override def iterator(context: IContext) = r1.iterator
      })
    
    val f2 = new Factor(new Prototype("j", classOf[Int]), new IDomain[Int] {
        override def iterator(context: IContext) = r2.iterator
      })

    val f3 = new Factor(new Prototype("k", classOf[Int]), new IDomain[Int] {
        override def iterator(context: IContext) = r3.iterator
      })    
    
    val sampling = new CompleteSampling(f1, f2, f3).build(Context.empty).map{ _.map{_.value}.toList }.toSet

    sampling.size should equal (r1.size * r2.size * r3.size)
    for(i <- r1 ; j <- r2 ; k <- r3) sampling.contains(List(i, j, k)) should equal (true)
  }
  
  "Complete sampling containing a factor with an empty domain" should "be empty" in {
    
    val r1 = (1 to 3)
    val r2 = Iterable.empty
    
    val f1 = new Factor(new Prototype("i", classOf[Int]), new IDomain[Int] {
        override def iterator(context: IContext) = r1.iterator
      })
    
    val f2 = new Factor(new Prototype("j", classOf[Int]), new IDomain[Int] {
        override def iterator(context: IContext) = r2.iterator
      })
  
    
    val sampling = new CompleteSampling(f1, f2).build(Context.empty).map{ _.map{_.value}.toList }.toSet

    sampling.isEmpty should equal (true)
  }
}
