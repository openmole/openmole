package org.openmole.core.dsl.extension

import org.openmole.core.context.Val

import scala.reflect.ClassTag
import scala.util.Random

object SeedVariable {

  def empty = new SeedVariable {
    type T = None.type
    def apply(rng: Random) = None
    def prototype = None
    def array(size: Int, rng: Random) = None
  }

  implicit def prototypeToSeeder[P: ClassTag](p: Val[P])(implicit seed: SeedType[P]): SeedVariable = new SeedVariable {
    type T = P
    def apply(rng: Random) = Some(Variable(p, seed(rng)))
    def array(size: Int, rng: Random) = {
      val valArray: Val[Array[T]] = p.toArray
      val valueArray: Array[T] = Iterator.continually(seed(rng)).take(size).toArray
      Some(org.openmole.core.context.Variable[Array[T]](valArray, valueArray))
    }
    def prototype = Some(p)
  }

  implicit def noneToSeeder(none: None.type): SeedVariable = empty

  object SeedType {
    implicit val longIsSeed: SeedType[Long] = new SeedType[Long] {
      override def apply(rng: Random): Long = rng.nextLong()
    }

    implicit val intIsSeed: SeedType[Int] = new SeedType[Int] {
      override def apply(rng: Random): Int = rng.nextInt()
    }
  }

  trait SeedType[T] {
    def apply(rng: Random): T
  }

}

trait SeedVariable {
  type T
  def apply(rng: Random): Option[Variable[T]]
  def prototype: Option[Val[T]]
  def array(size: Int, rng: Random): Option[Variable[Array[T]]]
}
