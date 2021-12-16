package org.openmole.tool.types

import scala.reflect.ClassTag

object ToArray {

  implicit def iterableToArray: ToArray[Iterable] = new ToArray[Iterable] {
    override def apply[T: ClassTag](a: Iterable[T]): Array[T] = a.toArray
  }

  implicit def arrayToArray: ToArray[Array] = new ToArray[Array] {
    override def apply[T: ClassTag](a: Array[T]): Array[T] = a
  }

}

trait ToArray[-A[_]] {
  def apply[T: ClassTag](a: A[T]): Array[T]
}
