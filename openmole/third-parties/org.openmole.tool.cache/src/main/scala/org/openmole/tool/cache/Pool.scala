package org.openmole.tool.cache

class Pool[T](f: ⇒ T) {

  var instances: List[T] = Nil

  def borrow: T = synchronized {
    instances match {
      case head :: tail ⇒
        instances = tail
        head
      case Nil ⇒ f
    }
  }

  def release(t: T) = synchronized { instances ::= t }
  def discard(t: T) = {}

  def exec[A](f: T ⇒ A): A = {
    val o = borrow
    try f(o)
    finally release(o)
  }
}
