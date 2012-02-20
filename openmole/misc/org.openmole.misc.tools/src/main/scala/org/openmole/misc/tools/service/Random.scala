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

import java.util.UUID
import org.apache.commons.math3.random.{RandomGenerator, Well44497b, RandomAdaptor}
import scala.collection.mutable.IndexedSeq

object Random {
  implicit def uuid2long(uuid: UUID) = uuid.getMostSignificantBits ^ uuid.getLeastSignificantBits
  
  val default = buildSynchronized(UUID.randomUUID)
  
  def buildSynchronized(seed: Long) = new SynchronizedRandom(new Well44497b(seed))
  
  def build(seed: Long) = new RandomAdaptor(new Well44497b(seed))
  
  class SynchronizedRandom(generator: RandomGenerator) extends java.util.Random {
    override def nextBoolean = synchronized { generator.nextBoolean }
    override def nextBytes(bytes: Array[Byte]) = synchronized { generator.nextBytes(bytes) }
    override def nextDouble = synchronized { generator.nextDouble }
    override def nextFloat = synchronized { generator.nextFloat }
    override def nextGaussian = synchronized { generator.nextGaussian }
    override def nextInt = synchronized { generator.nextInt }
    override def nextInt(n: Int) = synchronized { generator.nextInt(n) }
    override def nextLong = synchronized { generator.nextLong }
    override def setSeed(seed: Long) = synchronized { generator.setSeed(seed) }
  }
  
  @transient lazy val longInterval = {
    val min = BigDecimal(Long.MinValue)
    val max = BigDecimal(Long.MaxValue) + 1
    max - min
  }
  
  
  implicit def randomDecorator(rng: java.util.Random) = new {
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
      val v = BigDecimal(rng.nextLong)
      ((v - Long.MinValue) * (BigDecimal(max) / longInterval)).toLong
    }
  }
  
}


