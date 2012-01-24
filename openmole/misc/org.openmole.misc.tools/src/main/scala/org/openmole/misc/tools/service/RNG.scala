/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.misc.tools.service

import java.util.Random
import java.util.UUID
import org.apache.commons.math.random.{RandomGenerator, Well44497b, RandomAdaptor}
import scala.collection.mutable.IndexedSeq

object RNG {
  
  val rng = new RandomAdaptor(new RandomGenerator {
      val generator = new Well44497b( {
          val uuid = UUID.randomUUID
          uuid.getMostSignificantBits ^ uuid.getLeastSignificantBits
      })
    
    def nextBoolean = synchronized { generator.nextBoolean }
    def	nextBytes(bytes: Array[Byte]) = synchronized { generator.nextBytes(bytes) }
    def nextDouble = synchronized { generator.nextDouble }
    def nextFloat = synchronized { generator.nextFloat }
    def	nextGaussian = synchronized { generator.nextGaussian }
    def nextInt = synchronized { generator.nextInt }
    def nextInt(n: Int) = synchronized { generator.nextInt(n) }
  })
  
  @transient lazy val longInterval = {
    val min = BigDecimal(Long.MinValue)
    val max = BigDecimal(Long.MaxValue) + 1
    max - min
  }
  
  implicit def randomDecorator(rng: Random) = new {
    def shuffle[T](a: IndexedSeq[T]) ={
      for (i <- 1 until a.size reverse) {
        val j = rng.nextInt (i + 1)
        val t = a(i)
        a(i) = a(j)
        a(j) = t
      }
      a
    }
  
    def nextLong(max: Long): Long = {
      val v = BigDecimal(rng.nextLong())
      ((v - Long.MinValue) * (BigDecimal(max) / longInterval)).toLong
    }
  }
  
}
