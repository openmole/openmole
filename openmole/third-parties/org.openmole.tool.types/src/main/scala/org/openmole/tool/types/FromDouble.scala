package org.openmole.tool.types

object FromDouble {

  def apply[T](f: Double ⇒ T): FromDouble[T] = new FromDouble[T] {
    override def apply(s: Double) = f(s)
  }

  implicit def doubleToInt = FromDouble[Int](_.toInt)
  implicit def longToDouble = FromDouble[Long](_.toLong)
  implicit def floatToDouble = FromDouble[Float](_.toFloat)
  implicit def doubleToDouble = FromDouble[Double](identity)

}

trait FromDouble[+T] {
  def apply(d: Double): T
}
