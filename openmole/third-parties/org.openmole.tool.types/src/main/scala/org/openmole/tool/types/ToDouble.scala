package org.openmole.tool.types

object ToDouble:

  inline def apply[T](inline f: T => Double): ToDouble[T] =
    new ToDouble[T]:
      override def apply(t: T) = f(t)
  
  given ToDouble[Int] = ToDouble[Int](_.toDouble)
  given ToDouble[Long] = ToDouble[Long](_.toDouble)
  given ToDouble[Float] = ToDouble[Float](_.toDouble)
  given ToDouble[Double] = ToDouble[Double](identity)


trait ToDouble[T]:
  def apply(t: T): Double
