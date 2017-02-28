package org.openmole.tool.types

object ToDouble {

  def apply[T](f: T ⇒ Double): ToDouble[T] = new ToDouble[T] {
    override def apply(t: T) = f(t)
  }

  implicit def intToDouble = ToDouble[Int](_.toDouble)
  implicit def longToDouble = ToDouble[Long](_.toDouble)
  implicit def floatToDouble = ToDouble[Float](_.toDouble)
  implicit def doubleToDouble = ToDouble[Double](identity)

}

trait ToDouble[T] {
  def apply(t: T): Double
}
